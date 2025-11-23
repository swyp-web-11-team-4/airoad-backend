package com.swygbro.airoad.backend.ai.agent.trip.v2.evaluator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.common.dto.EvaluationResponse;
import com.swygbro.airoad.backend.ai.agent.common.evaluator.Evaluator;
import com.swygbro.airoad.backend.ai.agent.common.evaluator.Evaluator.EvaluationResult.Evaluation;
import com.swygbro.airoad.backend.ai.agent.common.evaluator.Evaluator.EvaluationResult.Issue;
import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse.AiScheduledPlace;
import com.swygbro.airoad.backend.ai.agent.trip.v2.context.TripPlanContext;
import com.swygbro.airoad.backend.ai.agent.trip.v2.optimizer.TripPlanIssueType;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;

import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Stream.concat;

@Slf4j
@Component
public class TripPlanEvaluator implements Evaluator<TripPlanContext, TripPlanIssueType> {

  private final ChatClient chatClient;

  private final PromptTemplate evaluationPromptTemplate =
      new PromptTemplate(
          """
          당신은 여행 일정 평가자(Travel Auditor)입니다.
          제시된 [생성된 일정]이 [평가 기준]을 충족하는지 검증하세요.

          ## 유저 요구사항
          - 여행 지역: {region}
          - 선호 테마: {themes}
          - 이동 수단: {transportation}

          ## 생성된 일정 상세 정보
          {placeDetails}

          ## 평가 기준

          1. **카테고리 구성 확인**
             다음의 일정 순서를 지키고 있는가?
             - 1. MORNING (오전 일정)
             - 2. LUNCH (점심 식사)
             - 3. AFTERNOON (오후 일정)
             - 4. DINNER (저녁 식사)
             - 5. EVENING (저녁 일정)

          2. **일정 개수 확인**
             정확히 5개여야 함

          3. **식사 장소 검증**
             점심, 저녁 식사 시간에 배치된 장소는 반드시 음식점이어야 함
             - 카페는 음식점으로 취급하지 않음

          4. **이동 시간 검증**
             각 일정 사이의 예상 이동 시간이 1시간 이내여야 함
             - 1시간을 초과하는 구간이 있으면 FAIL
             - 각 이동 시간을 명시적으로 확인하세요
             - MOVEMENT_TIME_EXCEEDED 이슈 발견 시, 이슈 메시지에 문제 구간의 출발지와 도착지 주소를 반드시 포함하세요
               예: "[주소1] 에서 [주소2] 로 이동: 71분으로 11분 초과 (1시간 제한)"

          ## 이슈 타입
          문제가 발견되면 다음 5가지 타입 중 **정확히 하나**를 선택하세요:
          {issueGuide}

          ## 출력 형식
          - 위 기준 1-4를 모두 만족하면: PASS
          - 하나라도 만족하지 않으면: NEEDS_IMPROVEMENT + 발견된 이슈들을 위 5가지 타입으로만 분류
          - 주관적 의견(예: "저녁이 너무 바쁠 것 같다")은 절대 포함하지 마세요
          - 객관적 사실만 기술 (어디가 문제인지, 왜 기준을 벗어났는지)
          """);

  public TripPlanEvaluator(@Qualifier("openAiChatModel") ChatModel chatModel) {
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultAdvisors(
                StructuredOutputValidationAdvisor.builder()
                    .outputType(
                        new ParameterizedTypeReference<EvaluationResponse<TripPlanIssueType>>() {})
                    .maxRepeatAttempts(3)
                    .build())
            .build();
  }

  @Override
  public EvaluationResult<TripPlanIssueType> evaluate(TripPlanContext context) {
    AiDailyPlanRequest request = context.getRequest();
    AiDailyPlanResponse plan = context.getDailyPlan();

    if (plan == null || plan.places() == null) {
      return new EvaluationResult<>(
          EvaluationResult.Evaluation.NEEDS_IMPROVEMENT,
          List.of(new Issue<>(TripPlanIssueType.INVALID_SCHEDULE_COUNT, "일정이 생성되지 않았습니다")));
    }

    List<Document> documents =
        concat(
                context.getSearchResults().places().stream(),
                context.getSearchResults().restaurants().stream())
            .distinct()
            .toList();

    Map<Long, Document> documentMap =
        documents.stream()
            .collect(
                Collectors.toMap(
                    document -> ((Integer) document.getMetadata().get("placeId")).longValue(),
                    Function.identity(),
                    (existing, replacement) -> existing));

    String placeDetails =
        plan.places().stream()
            .map(place -> formatPlaceDetail(place, documentMap.get(place.placeId())))
            .collect(Collectors.joining("\n"));

    String issueGuide =
        Arrays.stream(TripPlanIssueType.values())
            .map(
                issueType ->
                    String.format("- %s: %s", issueType.name(), issueType.getDescription()))
            .collect(Collectors.joining("\n          "));

    String prompt =
        evaluationPromptTemplate.render(
            Map.of(
                "region", request.region(),
                "themes",
                    request.themes().stream()
                        .map(PlaceThemeType::getDescription)
                        .collect(Collectors.joining(", ")),
                "transportation", request.transportation().getDescription(),
                "placeDetails", placeDetails,
                "issueGuide", issueGuide));

    EvaluationResponse<TripPlanIssueType> evaluationResponse =
        chatClient.prompt().user(prompt).call().entity(new ParameterizedTypeReference<>() {});

    EvaluationResult<TripPlanIssueType> evaluation =
        new EvaluationResult<>(
            Objects.requireNonNull(evaluationResponse).evaluation()
                    == EvaluationResponse.Evaluation.PASS
                ? Evaluation.PASS
                : Evaluation.NEEDS_IMPROVEMENT,
            evaluationResponse.issues().stream()
                .map(i -> new Issue<>(i.type(), i.message()))
                .toList());

    log.info("=== EVALUATOR OUTPUT ===");
    log.info("평가: {}", evaluation.evaluation());
    if (!evaluation.issues().isEmpty()) {
      evaluation.issues().forEach(issue -> log.info("  - [{}] {}", issue.type(), issue.message()));
    }

    return evaluation;
  }

  private String formatPlaceDetail(AiScheduledPlace schedule, Document originalDoc) {
    if (originalDoc == null) {
      return String.format(
          "%d. ID:%d (정보 없음 - 원본 문서 소실됨)", schedule.visitOrder(), schedule.placeId());
    }

    Map<String, Object> meta = originalDoc.getMetadata();

    String name = (String) meta.getOrDefault("name", "이름 없음");
    String themes = String.valueOf(meta.getOrDefault("themes", "미분류"));
    String address = (String) meta.getOrDefault("address", "");

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
        name,
        themes,
        schedule.category().getDescription(),
        address,
        schedule.travelTime(),
        schedule.transportation().getDescription(),
        originalDoc.getText() != null ? originalDoc.getText() : "");
  }
}
