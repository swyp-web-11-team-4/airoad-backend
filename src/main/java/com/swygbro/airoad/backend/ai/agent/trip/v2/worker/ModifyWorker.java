package com.swygbro.airoad.backend.ai.agent.trip.v2.worker;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.common.evaluator.Evaluator.EvaluationResult;
import com.swygbro.airoad.backend.ai.agent.common.evaluator.Evaluator.EvaluationResult.Issue;
import com.swygbro.airoad.backend.ai.agent.common.worker.Worker;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse.AiScheduledPlace;
import com.swygbro.airoad.backend.ai.agent.trip.v2.context.TripPlanContext;
import com.swygbro.airoad.backend.ai.agent.trip.v2.optimizer.TripPlanIssueType;
import com.swygbro.airoad.backend.content.application.PlaceQueryUseCase;
import com.swygbro.airoad.backend.content.domain.dto.response.PlaceResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ModifyWorker implements Worker<TripPlanContext, TripPlanTaskType> {

  private final ChatClient chatClient;
  private final PlaceQueryUseCase placeQueryUseCase;

  public ModifyWorker(
      @Qualifier("openAiChatModel") ChatModel chatModel, PlaceQueryUseCase placeQueryUseCase) {
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultAdvisors(
                StructuredOutputValidationAdvisor.builder()
                    .outputType(AiDailyPlanResponse.class)
                    .maxRepeatAttempts(3)
                    .build())
            .build();
    this.placeQueryUseCase = placeQueryUseCase;
  }

  @Override
  public TripPlanTaskType getTaskType() {
    return TripPlanTaskType.MODIFY;
  }

  private final PromptTemplate modificationPromptTemplate =
      new PromptTemplate(
          """
      당신은 여행 일정 수정 전문가입니다.
      `문제점`을 해결하기 위해, 현재 일정의 문제 있는 장소를 교체 가능한 후보 장소 목록에 있는 **더 나은 장소**로 교체하세요.

      ## 문제점
      {issues}

      ## 현재 일정
      {currentPlan}

      ## 교체 가능한 장소 목록
      ### 장소 관련 목록
      {newPlaces}

      ### 음식점 관련 목록
      {newRestaurants}

      ## 필수 제약 사항
      1. **절대 거짓 정보를 생성하지 마십시오.**
         - 반드시 위 `후보 목록`에 있는 장소의 `ID` 정보만 사용해야 합니다.
         - 목록에 없는 장소를 절대 사용하지 마세요.
      2. **일정 구성 로직**:
         - `오전 일정 -> 점심 식사 -> 오후 일정 -> 저녁 식사 -> 저녁 일정`의 흐름을 기본으로 하세요.
           - 1. MORNING (오전 일정)
           - 2. LUNCH (점심 식사 - 반드시 음식점)
           - 3. AFTERNOON (오후 일정)
           - 4. DINNER (저녁 식사 - 반드시 음식점)
           - 5. EVENING (저녁 일정)
         - 총 5개의 스팟을 배치하세요.
      3. **동선 최적화**:
         - 제공된 `주소(Address)`를 참고하여 서로 비슷한 주소끼리 묶으세요.
         - 동선이 꼬이지 않도록 지리적으로 인접한 순서대로 방문하게 하세요.
         - 이동 수단은 반드시 대중교통으로 고정하고, 이동 시간은 `0`으로 입력합니다. (DB에서 자동 계산됨)
      4. **식사 장소**:
         - 제공된 음식점 후보지에 카페가 있더라도 선택하지 않습니다.
         - 반드시 식사에 적합한 음식점을 선택하세요.

      ## 수정 규칙
      1. **적극적 교체**: 지적된 문제(예: 식사 시간에 카페 배치)를 해결하기 위해, 후보 목록에서 **가장 적절한 새 장소**를 찾아 교체하세요.
      2. **후보 우선순위**: 만약 [후보 장소 목록]에 피드백 내용을 반영한(예: 맛집) 장소가 있다면, 기존 장소 대신 그것을 최우선으로 사용하세요.
      3. **보존 원칙**: 문제가 없는 나머지 일정은 건드리지 말고 원본을 유지하세요.
      4. **이동 수단**: 이동 수단은 반드시 대중교통으로 고정하고, 이동 시간은 `0`으로 입력합니다. (DB에서 자동 계산됨)
      5. dayNumber는 {dayNumber}로 유지하세요.
      """);

  @Override
  public void execute(TripPlanContext context) {
    AiDailyPlanResponse currentPlan = context.getDailyPlan();
    List<Document> places = context.getSearchResults().places();
    List<Document> restaurants = context.getSearchResults().restaurants();
    EvaluationResult<TripPlanIssueType> evaluationResult = context.getEvaluationResult();

    if (currentPlan == null) {
      log.warn("수정할 원본 일정이 없습니다.");
      return;
    }

    List<Long> placeIds = currentPlan.places().stream().map(AiScheduledPlace::placeId).toList();

    Map<Long, PlaceResponse> placeMap =
        placeQueryUseCase.findAllPlaceById(placeIds).stream()
            .collect(Collectors.toMap(PlaceResponse::id, Function.identity()));

    String currentPlanText =
        currentPlan.places().stream()
            .map(schedule -> formatCurrentPlaceDetail(schedule, placeMap.get(schedule.placeId())))
            .collect(Collectors.joining("\n"));

    String issueText = "수정할 이슈가 없습니다.";
    if (evaluationResult != null) {
      List<Issue<TripPlanIssueType>> issues = evaluationResult.issues();
      if (!issues.isEmpty()) {
        issueText =
            issues.stream()
                .map(i -> String.format("- [%s] %s", i.type(), i.message()))
                .collect(Collectors.joining("\n"));
      }
    }

    String prompt =
        modificationPromptTemplate.render(
            Map.of(
                "currentPlan", currentPlanText,
                "issues", issueText,
                "newPlaces", convertDocumentToString(places),
                "newRestaurants", convertDocumentToString(restaurants),
                "dayNumber", String.valueOf(context.getDayNumber())));

    AiDailyPlanResponse refinedPlan =
        chatClient.prompt().user(prompt).call().entity(AiDailyPlanResponse.class);

    log.info("일정 수정 완료");
    context.setDailyPlan(refinedPlan);
  }

  private String convertDocumentToString(List<Document> documents) {
    return documents.stream().map(this::formatDocument).collect(Collectors.joining("\n"));
  }

  private String formatCurrentPlaceDetail(AiScheduledPlace schedule, PlaceResponse place) {
    if (place == null) {
      return String.format(
          "%d. ID:%d (정보 없음 - 원본 데이터 소실됨)", schedule.visitOrder(), schedule.placeId());
    }

    return String.format(
        """
            %d. %s
              - 테마: %s
              - 일정 카테고리: %s
              - 주소: %s
              - 다음 장소 예상 이동시간: %d분 (이동수단: %s)
              - 장소 설명: %s
            """,
        schedule.visitOrder(),
        place.name(),
        place.themes(),
        schedule.category().getDescription(),
        place.address(),
        schedule.travelTime(),
        schedule.transportation().getDescription(),
        place.description() != null ? place.description() : "");
  }

  private String formatDocument(Document doc) {
    Map<String, Object> meta = doc.getMetadata();
    return String.format(
        "[ID:%s] %s (테마:%s, 주소:%s)",
        meta.get("placeId"), meta.get("name"), meta.get("themes"), meta.get("address"));
  }
}
