package com.swygbro.airoad.backend.ai.agent.trip.step;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.trip.context.ExecutionContext;
import com.swygbro.airoad.backend.ai.agent.trip.context.TripPlanContextKey;
import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.DeduplicationResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.RouteOptimizationResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.RouteOptimizationResponse.RoutedPlace;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.ScheduleSummaryResponse;
import com.swygbro.airoad.backend.ai.agent.trip.pipeline.PipelineStep;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ScheduleSummaryStep implements PipelineStep {

  private final ChatClient chatClient;

  public ScheduleSummaryStep(@Qualifier("openAiChatModel") ChatModel chatModel) {
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultAdvisors(
                StructuredOutputValidationAdvisor.builder()
                    .outputType(ScheduleSummaryResponse.class)
                    .maxRepeatAttempts(3)
                    .build())
            .build();
  }

  private final PromptTemplate summaryPromptTemplate =
      new PromptTemplate(
          """
      당신은 전문 여행 가이드입니다.
      아래 여행자 정보를 바탕으로 각 장소에 대한 맞춤형 요약과 전체 일정 설명을 생성하세요.

      <여행자정보>
      <인원>{peopleCount}명</인원>
      <테마>{themes}</테마>
      <일차>{dayNumber}일차</일차>
      <지역>{region}</지역>
      </여행자정보>

      <확정된일정>
      {scheduleWithPlaces}
      </확정된일정>

      <작성지침>
      1. 인원수에 맞는 표현 사용:
         - 1명: "혼자 여행하기", "1인 여행객에게", "조용히 즐기기"
         - 2명: "친구/커플과 함께", "둘이서 즐기기"
         - 3-4명: "가족 단위", "아이와 함께", "부모님과"
         - 5명 이상: "단체 여행", "모임", "회식"

      2. 테마 반영:
         - 각 장소 요약에 선택된 테마({themes})와의 연관성 강조
         - 예: 힐링 테마 → "여유로운", "평온한", "사색하기 좋은"

      3. 불필요한 일반화 금지:
         - 1인 여행인데 "가족과 함께" 같은 표현 절대 금지
         - 실제 여행자 정보에 맞는 표현만 사용
      </작성지침>
      """);

  @Override
  public ExecutionContext execute(ExecutionContext context) {
    RouteOptimizationResponse routeResult =
        context.get(TripPlanContextKey.ROUTE_OPTIMIZATION_RESULT);
    DeduplicationResponse dedupResult = context.get(TripPlanContextKey.DEDUP_RESULT);
    AiDailyPlanRequest request = context.get(TripPlanContextKey.REQUEST);
    Integer dayNumber = context.get(TripPlanContextKey.DAY_NUMBER);

    if (routeResult == null || routeResult.places().isEmpty()) {
      log.warn("경로 최적화 결과가 없음");
      return context;
    }

    List<Document> allDocuments = new ArrayList<>();
    allDocuments.addAll(dedupResult.places());
    allDocuments.addAll(dedupResult.restaurants());

    Map<Long, Document> documentMap =
        allDocuments.stream()
            .collect(
                Collectors.toMap(
                    doc -> ((Integer) doc.getMetadata().get("placeId")).longValue(),
                    doc -> doc,
                    (existing, replacement) -> existing));

    String scheduleText = buildScheduleText(routeResult.places(), documentMap);

    String themes =
        request.themes().stream()
            .map(PlaceThemeType::getDescription)
            .collect(Collectors.joining(", "));

    String prompt =
        summaryPromptTemplate.render(
            Map.of(
                "peopleCount", request.peopleCount().toString(),
                "themes", themes,
                "dayNumber", dayNumber.toString(),
                "region", request.region(),
                "scheduleWithPlaces", scheduleText));

    ChatClientRequestSpec chatClientRequest = chatClient.prompt().system(prompt);

    if (request.userMessage() != null && !request.userMessage().isBlank()) {
      chatClientRequest = chatClientRequest.user(request.userMessage());
    }

    ScheduleSummaryResponse summaryResult =
        chatClientRequest.call().entity(ScheduleSummaryResponse.class);

    log.info("일정 요약 생성 완료: {}", summaryResult.title());

    context.put(TripPlanContextKey.SCHEDULE_SUMMARY_RESULT, summaryResult);

    return context;
  }

  @Override
  public String getName() {
    return "ScheduleSummary";
  }

  /**
   * 프롬프트용 일정 정보 텍스트 생성
   *
   * @param routedPlaces 경로 최적화 결과
   * @param documentMap placeId -> Document 매핑
   * @return 일정 정보 텍스트
   */
  private String buildScheduleText(
      List<RoutedPlace> routedPlaces, Map<Long, Document> documentMap) {
    return routedPlaces.stream()
        .map(
            rp -> {
              Document doc = documentMap.get(rp.placeId());
              if (doc == null) {
                log.warn("장소 정보를 찾을 수 없음: placeId={}", rp.placeId());
                return String.format("%d. [%s] 장소 정보 없음", rp.visitOrder(), rp.category());
              }

              return String.format(
                  """
                  %d. [ID:%s] %s
                    카테고리: %s
                    설명: %s
                  """,
                  rp.visitOrder(),
                  rp.placeId(),
                  doc.getMetadata().get("name"),
                  rp.category().getDescription(),
                  doc.getText());
            })
        .collect(Collectors.joining("\n"));
  }
}
