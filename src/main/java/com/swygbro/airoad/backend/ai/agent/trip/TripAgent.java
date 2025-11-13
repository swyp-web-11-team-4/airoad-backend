package com.swygbro.airoad.backend.ai.agent.trip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.advisor.PromptMetadataAdvisor;
import com.swygbro.airoad.backend.ai.agent.common.AbstractPromptAgent;
import com.swygbro.airoad.backend.ai.agent.trip.converter.NdJsonBeanOutPutConverter;
import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse;
import com.swygbro.airoad.backend.ai.application.query.AiPromptTemplateQueryUseCase;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.event.DailyPlanGeneratedEvent;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.content.application.PlaceQueryUseCase;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
import com.swygbro.airoad.backend.trip.domain.dto.request.DailyPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceCreateRequest;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationCompletedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationErrorEvent;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import reactor.core.publisher.Flux;

@Slf4j
@Component
public class TripAgent extends AbstractPromptAgent {

  private final AgentType agentType = AgentType.TRIP_AGENT;

  private final NdJsonBeanOutPutConverter<AiDailyPlanResponse> outputConverter =
      new NdJsonBeanOutPutConverter<>(AiDailyPlanResponse.class);

  private final ChatClient chatClient;
  private final ApplicationEventPublisher eventPublisher;

  public TripAgent(
      ApplicationEventPublisher eventPublisher,
      @Qualifier("upstageChatModel") ChatModel chatModel,
      PlaceQueryUseCase placeQueryUseCase,
      AiPromptTemplateQueryUseCase promptTemplateQueryUseCase) {
    super(promptTemplateQueryUseCase);

    String jsonSchema = outputConverter.getFormat();

    this.eventPublisher = eventPublisher;
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultAdvisors(
                //                new SimpleLoggerAdvisor(),
                PromptMetadataAdvisor.builder()
                    .metadata(PromptMetadataAdvisor.systemMetadata(jsonSchema))
                    .build())
            .defaultTools(placeQueryUseCase)
            .build();
  }

  @Override
  public boolean supports(AgentType agentType) {
    return this.agentType == agentType;
  }

  @Override
  public void execute(Object data) {
    AiDailyPlanRequest request = (AiDailyPlanRequest) data;

    PromptPair prompts = findActivePromptPair(agentType);

    val params = convertParams(request);

    chatClient
        .prompt()
        .system(promptSystemSpec -> promptSystemSpec.text(prompts.systemPrompt()))
        .advisors(
            a ->
                a.param(
                    PromptMetadataAdvisor.METADATA_KEY,
                    PromptMetadataAdvisor.userMetadata(
                        """
                    ## 유저 여행 정보
                    %s
                    """
                            .formatted(params.toString()))))
        .user(promptUserSpec -> promptUserSpec.text(prompts.userPrompt()))
        .stream()
        .content()
        .doOnSubscribe(subscription -> log.debug("AI 일정 생성 스트림 구독 시작됨."))
        .transform(this::toJsonChunk)
        .mapNotNull(
            jsonLine -> {
              try {
                log.debug("파싱 시도: {}", jsonLine);
                return outputConverter.convert(jsonLine);
              } catch (Exception e) {
                TripPlanGenerationErrorEvent event =
                    TripPlanGenerationErrorEvent.builder()
                        .chatRoomId(request.chatRoomId())
                        .tripPlanId(request.tripPlanId())
                        .username(request.username())
                        .errorCode(AiErrorCode.JSON_PARSING_FAILED)
                        .build();

                eventPublisher.publishEvent(event);
                log.error("JSON 스트림 파싱 오류: {}", e.getMessage());
                return null;
              }
            })
        .filter(Objects::nonNull)
        .doOnNext(
            aiDailyPlanResponse -> {
              DailyPlanCreateRequest dailyPlanCreateRequest =
                  toDailyPlanDto(Objects.requireNonNull(aiDailyPlanResponse));

              DailyPlanGeneratedEvent event =
                  DailyPlanGeneratedEvent.builder()
                      .chatRoomId(request.chatRoomId())
                      .tripPlanId(request.tripPlanId())
                      .username(request.username())
                      .dailyPlan(dailyPlanCreateRequest)
                      .build();

              eventPublisher.publishEvent(event);
              log.info("Day {} 일정 생성 완료", dailyPlanCreateRequest.dayNumber());
            })
        .doOnError(
            error -> {
              TripPlanGenerationErrorEvent event =
                  TripPlanGenerationErrorEvent.builder()
                      .chatRoomId(request.chatRoomId())
                      .tripPlanId(request.tripPlanId())
                      .username(request.username())
                      .errorCode(AiErrorCode.TRIP_PLAN_GENERATION_ERROR)
                      .build();

              eventPublisher.publishEvent(event);
              log.error("AI 일정 생성 스트림 처리 중 오류 발생", error);
            })
        .doOnComplete(
            () -> {
              TripPlanGenerationCompletedEvent event =
                  TripPlanGenerationCompletedEvent.builder()
                      .chatRoomId(request.chatRoomId())
                      .tripPlanId(request.tripPlanId())
                      .username(request.username())
                      .message("AI 여행 일정 생성 요청에 성공했습니다")
                      .build();

              eventPublisher.publishEvent(event);
              log.debug("AI 일정 생성 스트림 처리 완료.");
            })
        .subscribe();
  }

  /**
   * 여행 일정 생성 요청 Dto를 프롬프트 템플릿에 맞게 Map으로 변환합니다.
   *
   * @param request 유저에게 받은 요청 Dto
   * @return 변환된 Parameter Map
   */
  private Map<String, Object> convertParams(AiDailyPlanRequest request) {
    val params = new HashMap<String, Object>();
    val daysDescription = new StringBuilder();
    val days = request.duration();

    for (int i = 0; i < days; i++) {
      val date = request.startDate().plusDays(i);
      daysDescription.append(String.format("  - %d일차: %s%n", i + 1, date));
    }

    params.put("region", request.region());
    params.put("days", days);
    params.put("startDate", request.startDate());
    params.put("endDate", request.startDate().plusDays(days - 1));
    params.put(
        "themes",
        String.join("|", request.themes().stream().map(PlaceThemeType::getDescription).toList()));
    params.put("peopleCount", request.peopleCount());
    params.put("transportation", Transportation.PUBLIC_TRANSIT); // 이번 MVP에서는 대중 교통으로 고정
    params.put("daysDescription", daysDescription.toString());

    return params;
  }

  /**
   * 토큰 스트림을 NDJSON 형식의 청크로 변환합니다. 줄바꿈(\n)을 기준으로 각 JSON 객체를 분리합니다.
   *
   * @param tokenStream LLM으로부터 받은 토큰 스트림
   * @return JSON 문자열로 변환된 Flux
   */
  private Flux<String> toJsonChunk(Flux<String> tokenStream) {
    return Flux.defer(
        () -> {
          final StringBuilder buffer = new StringBuilder();
          return tokenStream
              .concatMap(
                  token -> {
                    buffer.append(token);

                    List<String> lines = new ArrayList<>();
                    int idx;

                    while ((idx = buffer.indexOf("\n")) >= 0) {
                      String line = buffer.substring(0, idx).trim();

                      if (line.startsWith("{") && line.endsWith("}")) {
                        lines.add(line);
                      }

                      buffer.delete(0, idx + 1);
                    }

                    return Flux.fromIterable(lines);
                  })
              .concatWith(
                  Flux.defer(
                      () -> {
                        String remaining = buffer.toString().trim();
                        if (remaining.startsWith("{") && remaining.endsWith("}")) {
                          return Flux.just(remaining);
                        }
                        return Flux.empty();
                      }));
        });
  }

  private DailyPlanCreateRequest toDailyPlanDto(AiDailyPlanResponse aiDailyPlanResponse) {
    List<ScheduledPlaceCreateRequest> scheduledPlaces =
        aiDailyPlanResponse.places().stream()
            .map(
                p ->
                    ScheduledPlaceCreateRequest.builder()
                        .placeId(p.placeId())
                        .visitOrder(p.visitOrder())
                        .category(p.category())
                        .startTime(p.startTime())
                        .endTime(p.endTime())
                        .travelTime(p.travelTime())
                        .transportation(p.transportation())
                        .build())
            .toList();

    return DailyPlanCreateRequest.builder()
        .dayNumber(aiDailyPlanResponse.dayNumber())
        .date(aiDailyPlanResponse.date())
        .title(aiDailyPlanResponse.title())
        .description(aiDailyPlanResponse.description())
        .places(scheduledPlaces)
        .build();
  }
}
