package com.swygbro.airoad.backend.ai.agent.trip;

import java.time.LocalDate;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor.MetadataEntry;
import com.swygbro.airoad.backend.ai.common.agent.AiroadAgent;
import com.swygbro.airoad.backend.ai.common.context.ContextManager;
import com.swygbro.airoad.backend.ai.domain.dto.context.PlaceVectorQueryContext;
import com.swygbro.airoad.backend.ai.domain.dto.context.TripPlanCommandContext;
import com.swygbro.airoad.backend.ai.domain.dto.context.TripPlanQueryContext;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.event.DailyPlanGeneratedEvent;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
import com.swygbro.airoad.backend.trip.domain.dto.request.DailyPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceCreateRequest;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationCompletedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationErrorEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TripAgent implements AiroadAgent {

  private final AgentType agentType = AgentType.TRIP_AGENT;
  private final ChatClient chatClient;
  private final ApplicationEventPublisher eventPublisher;
  private final ContextManager contextManager;

  public TripAgent(
      ApplicationEventPublisher eventPublisher,
      @Qualifier("openAiChatModel") ChatModel chatModel,
      ContextManager contextManager) {

    this.eventPublisher = eventPublisher;
    this.contextManager = contextManager;
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultAdvisors(PromptMetadataAdvisor.builder().build())
            .build();
  }

  @Override
  public boolean supports(AgentType agentType) {
    return this.agentType == agentType;
  }

  @Override
  public void execute(Object data) {
    AiDailyPlanRequest request = (AiDailyPlanRequest) data;

    log.info(
        "여행 일정 생성 시작 - 지역: {}, 기간: {}일, 여행 ID: {}",
        request.region(),
        request.duration(),
        request.tripPlanId());

    try {
      // 일차별 순차 생성
      for (int dayNumber = 1; dayNumber <= request.duration(); dayNumber++) {
        log.debug("{}일차 일정 생성 시작", dayNumber);

        AiDailyPlanResponse dailyPlan = generateDailyPlan(request, dayNumber);

        // 생성된 일정을 이벤트로 발행
        DailyPlanCreateRequest dailyPlanCreateRequest = toDailyPlanDto(dailyPlan);
        DailyPlanGeneratedEvent event =
            DailyPlanGeneratedEvent.builder()
                .chatRoomId(request.chatRoomId())
                .tripPlanId(request.tripPlanId())
                .username(request.username())
                .dailyPlan(dailyPlanCreateRequest)
                .build();

        eventPublisher.publishEvent(event);
        log.info("{}일차 일정 생성 완료", dayNumber);
      }

      // 전체 일정 생성 완료 이벤트 발행
      TripPlanGenerationCompletedEvent completedEvent =
          TripPlanGenerationCompletedEvent.builder()
              .chatRoomId(request.chatRoomId())
              .tripPlanId(request.tripPlanId())
              .username(request.username())
              .message("AI 여행 일정 생성 요청에 성공했습니다")
              .build();

      eventPublisher.publishEvent(completedEvent);
      log.info("여행 일정 전체 생성 완료 - 총 {}일", request.duration());

    } catch (Exception e) {
      log.error("AI 일정 생성 중 오류 발생", e);

      TripPlanGenerationErrorEvent errorEvent =
          TripPlanGenerationErrorEvent.builder()
              .chatRoomId(request.chatRoomId())
              .tripPlanId(request.tripPlanId())
              .username(request.username())
              .errorCode(AiErrorCode.TRIP_PLAN_GENERATION_ERROR)
              .build();

      eventPublisher.publishEvent(errorEvent);
    }
  }

  /**
   * 특정 일차의 여행 일정을 생성합니다.
   *
   * @param request 여행 일정 생성 요청
   * @param dayNumber 생성할 일차 번호 (1부터 시작)
   * @return 생성된 일일 일정
   */
  private AiDailyPlanResponse generateDailyPlan(AiDailyPlanRequest request, int dayNumber) {
    LocalDate targetDate = request.startDate().plusDays(dayNumber - 1);

    TripPlanQueryContext tripPlanQueryContext =
        TripPlanQueryContext.builder()
            .tripPlanId(request.tripPlanId())
            .username(request.username())
            .build();

    TripPlanCommandContext tripPlanCommandContext =
        TripPlanCommandContext.builder()
            .region(request.region())
            .startDate(request.startDate())
            .duration(request.duration())
            .themes(request.themes())
            .peopleCount(request.peopleCount())
            .transportation(request.transportation())
            .build();

    PlaceVectorQueryContext placeVectorQueryContext =
        PlaceVectorQueryContext.builder()
            .region(request.region())
            .themes(request.themes().stream().map(PlaceThemeType::getDescription).toList())
            .topK(10)
            .similarityThreshold(0.4d)
            .build();

    List<MetadataEntry> contextMetadata =
        contextManager.buildContext(
            AgentType.TRIP_AGENT,
            tripPlanQueryContext,
            tripPlanCommandContext,
            placeVectorQueryContext);

    String daySpecificPrompt =
        String.format(
            """
            %d일차 여행 (%s) 일정을 생성해주세요.
            """, dayNumber, targetDate);

    try {
      AiDailyPlanResponse dailyPlan =
          chatClient
              .prompt()
              .user(daySpecificPrompt)
              .advisors(a -> a.param(PromptMetadataAdvisor.METADATA_KEY, contextMetadata))
              .call()
              .entity(AiDailyPlanResponse.class);

      log.debug("{}일차 AI 응답 수신 완료", dayNumber);
      return dailyPlan;

    } catch (Exception e) {
      log.error("{}일차 일정 생성 중 오류 발생", dayNumber, e);
      throw new RuntimeException("일정 생성 실패: " + dayNumber + "일차", e);
    }
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
