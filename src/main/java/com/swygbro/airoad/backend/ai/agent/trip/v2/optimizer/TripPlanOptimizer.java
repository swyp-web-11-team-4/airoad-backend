package com.swygbro.airoad.backend.ai.agent.trip.v2.optimizer;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.common.dto.OptimizerResponse;
import com.swygbro.airoad.backend.ai.agent.common.evaluator.Evaluator.EvaluationResult;
import com.swygbro.airoad.backend.ai.agent.common.optimizer.Optimizer;
import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse;
import com.swygbro.airoad.backend.ai.agent.trip.v2.context.TripPlanContext;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TripPlanOptimizer implements Optimizer<TripPlanContext, TripPlanIssueType> {

  private final ChatClient chatClient;

  private final PromptTemplate optimizerPromptTemplate =
      new PromptTemplate(
          """
          당신은 여행 일정 최적화자(Optimizer)입니다.
          평가자가 지적한 문제점을 분석하고, **무엇이 잘못되었는지**만 설명하세요.

          ## 발견한 문제점
          {issues}

          ## 유저 여행 정보
          - 지역: {region}
          - 테마: {themes}

          ### 현재 생성된 일정
          {plan}

          ## 출력 형식
          1. 각 이슈를 구체적으로 분석하세요
          2. "왜" 문제인지만 설명
          3. 이슈가 5개 이상이면 "전체 일정 재생성을 권장합니다"라고 명시
          4. 주관적 표현(예: "저녁이 바쁠 것 같다") 절대 금지
          5. 객관적 사실만 기술
          """);

  public TripPlanOptimizer(@Qualifier("openAiChatModel") ChatModel chatModel) {
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultAdvisors(
                StructuredOutputValidationAdvisor.builder()
                    .outputType(OptimizerResponse.class)
                    .maxRepeatAttempts(3)
                    .build())
            .build();
  }

  @Override
  public OptimizationPlan optimize(
      EvaluationResult<TripPlanIssueType> result, TripPlanContext context) {
    AiDailyPlanRequest request = context.getRequest();
    AiDailyPlanResponse response = context.getDailyPlan();

    String issuesText =
        result.issues().stream()
            .map(issue -> String.format("- [%s] %s", issue.type(), issue.message()))
            .collect(Collectors.joining("\n"));

    String prompt =
        optimizerPromptTemplate.render(
            Map.of(
                "issues", issuesText,
                "region", request.region(),
                "themes",
                    request.themes().stream()
                        .map(PlaceThemeType::getDescription)
                        .collect(Collectors.joining(", ")),
                "plan", response.toString()));

    OptimizerResponse optimizerResponse =
        chatClient.prompt().user(prompt).call().entity(OptimizerResponse.class);

    OptimizationPlan optimization =
        new OptimizationPlan(optimizerResponse.strategy(), optimizerResponse.recommendation());

    log.info("=== OPTIMIZER OUTPUT ===");
    log.info("개선 전략: {}", optimization.strategy());
    log.info("권장사항: {}", optimization.recommendation());

    return optimization;
  }
}
