package com.swygbro.airoad.backend.ai.agent.trip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.AiroadAgent;
import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse;
import com.swygbro.airoad.backend.ai.domain.event.DailyPlanGeneratedEvent;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
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
public class TripAgent implements AiroadAgent {

  private static final String NAME = "tripAgent";

  private static final String SYSTEM_PROMPT_TEMPLATE =
      """
          당신은 국내 여행 일정 계획 전문가입니다. 다음의 지침을 반드시 지켜서 여행 일정을 생성해주세요.

          ## 지침
          - 주어진 여행 정보를 바탕으로 여행 일정 계획을 구성하세요.
          - 여행의 시작지와 도착지는 반드시 공항, 기차역, 버스터미널 등 대중교통을 이용하기 편한 곳으로 해주세요.
          - 프롬프트로 주어진 정보 외의 내용은 응답할 수 없습니다.
          - 사용자가 방문하고자 하는 지역에 맞는 정보만 선택해서 구성하세요.

          ## 출력 형식: NDJSON (Newline Delimited JSON)
          **중요**: 반드시 NDJSON 형식으로 응답해야 합니다.
          - 각 줄(line)에는 정확히 하나의 완전한 JSON 객체만 포함합니다.
          - 각 JSON 객체는 줄바꿈 문자(\\n)로 구분됩니다.
          - 절대로 배열([])이나 부모 객체로 감싸지 마세요.
          - 각 JSON 객체는 독립적으로 파싱 가능해야 합니다.

          ## 출력 예시
          ```
          \\{"dayNumber": 1, "date": "2025-10-27", "description": "...", "places": [...]\\}
          \\{"dayNumber": 2, "date": "2025-10-28", "description": "...", "places": [...]\\}
          \\{"dayNumber": 3, "date": "2025-10-29", "description": "...", "places": [...]\\}
          ```

          ## JSON 스키마
          {jsonSchema}
          """;

  private static final String USER_PROMPT_TEMPLATE =
      """
          아래 조건에 맞춰 {region} 지역의 {days}일 여행 계획을 생성해주세요.

          ## 여행 정보
          - 여행 기간: {startDate} ~ {endDate} ({days}일)
          - 여행 테마: {themes}
          - 여행 인원: {peopleCount}명
          - 선호하는 이동 수단: {transportation}

          ## 필드 규칙
          - 'dayNumber': 1부터 N까지 순차적으로 증가
          - 'date': 'yyyy-MM-dd' 형식
          - 'title': 해당 일차의 소제목
          - 'description': 해당 일차의 요약 설명
            - 일정 카테고리 별로 일정에 대한 소개 내용 작성, 카테고리는 반드시 한글로 변환
              - [오전 일정, 점심 식사, 오후 일정, 카페, 저녁 식사, 저녁 일정]
            - 마크다운 문법 사용
            - 쌍따옴표(")는 절대 사용하지 마세요. 필요 시 작은따옴표(')로 대체하세요.
            - 모든 내용은 하나의 문자열 안에 포함되어야 합니다.
          - 'places': 방문 장소 배열
            - 'placeId': 장소 ID
            - 'visitOrder': 일정 방문 순서 (1부터 시작)
            - 'category': 일정 카테고리, 반드시 [MORNING, LUNCH, AFTERNOON, CAFE, DINNER, EVENING] 중 하나
            - 'startTime': 일정 시작 시간, 'HH:mm' 형식 (예: 09:00, 13:30)
            - 'endTime': 일정 종료 시간, 'HH:mm' 형식 (예: 11:00, 15:30)
            - 'travelTime': 이전 장소로부터의 이동 시간(분)
            - 'transportation': 반드시 [NONE, WALKING, PUBLIC_TRANSIT, CAR] 중 하나
          - 'nextQuestions': 유저가 질문하면 좋을 법한 질문 추천 배열
            - 'recommendedQuestion': 추천하는 질문 내용 (ex. "OOO 운영 시간에 대해 알려주세요", "다른 장소로 일정을 변경하고 싶어요")

          ## 일정 날짜
          {daysDescription}

          **중요**: 위의 모든 일차({days}일)에 대한 JSON 객체를 모든 필드를 반드시 포함해서 생성해야 합니다.

          여행 일정을 1일차부터 {days}일차까지 순서대로 생성해주세요.
          """;

  private final BeanOutputConverter<AiDailyPlanResponse> outputConverter =
      new BeanOutputConverter<>(AiDailyPlanResponse.class);

  private final ChatClient chatClient;

  private final ApplicationEventPublisher eventPublisher;

  private final VectorStore vectorStore;

  public TripAgent(
      ApplicationEventPublisher eventPublisher, ChatModel chatModel, VectorStore vectorStore) {
    this.eventPublisher = eventPublisher;
    this.vectorStore = vectorStore;
    String jsonSchema = outputConverter.getFormat();

    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultSystem(
                promptSystemSpec ->
                    promptSystemSpec
                        .text(SYSTEM_PROMPT_TEMPLATE)
                        .params(Map.of("jsonSchema", jsonSchema)))
            .build();
  }

  @Override
  public boolean supports(String agentName) {
    return NAME.equals(agentName);
  }

  @Override
  public void execute(Object data) {
    AiDailyPlanRequest request = (AiDailyPlanRequest) data;

    val params = convertParams(request);

    String filterExpression = buildFilterExpression(request);

    chatClient
        .prompt()
        .user(promptUserSpec -> promptUserSpec.text(USER_PROMPT_TEMPLATE).params(params))
        .advisors(
            QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(
                    SearchRequest.builder()
                        .topK(5)
                        .similarityThreshold(0.3d)
                        .filterExpression(filterExpression)
                        .build())
                .build())
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
              log.debug("Day {} 일정 생성 완료", dailyPlanCreateRequest.dayNumber());
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
   * 필터 표현식 생성: themes in ['테마1', '테마2', ...]
   *
   * <p>메타데이터 구조: - themes: 문자열 배열 (예: ["음식점", "유명 관광지"])
   *
   * @param request 여행 일정 요청
   * @return 문자열 형태의 필터 표현식
   */
  private String buildFilterExpression(AiDailyPlanRequest request) {
    List<PlaceThemeType> themes = request.themes();

    String themeList =
        themes.stream()
            .map(theme -> String.format("'%s'", theme.getDescription()))
            .collect(Collectors.joining(", ", "[", "]"));

    return String.format("themes in %s", themeList);
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
