package com.swygbro.airoad.backend.ai.agent.trip.v2.orchestrator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.common.dto.OrchestratorResponse;
import com.swygbro.airoad.backend.ai.agent.common.dto.WorkerTask;
import com.swygbro.airoad.backend.ai.agent.common.optimizer.Optimizer.OptimizationPlan;
import com.swygbro.airoad.backend.ai.agent.common.orchestrator.Orchestrator;
import com.swygbro.airoad.backend.ai.agent.common.worker.Worker;
import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.v2.context.TripPlanContext;
import com.swygbro.airoad.backend.ai.agent.trip.v2.worker.TripPlanTaskType;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TripPlanOrchestrator extends Orchestrator<TripPlanContext, TripPlanTaskType> {

  private final ChatClient chatClient;

  private final PromptTemplate taskPlanPromptTemplate =
      new PromptTemplate(
          """
          당신은 여행 일정 작업 계획자(Orchestrator)입니다.
          현재 단계에 맞는 **필요한 Worker 작업 리스트**를 효율적으로 계획하세요.

          ## 여행 조건
          - 지역: {region}
          - 일차: {dayNumber}일차
          - 테마: {themes}

          ## 현재 실행 단계
          {phase}

          ## 피드백 및 개선 방향
          {feedback}

          ## 작업 계획 수립 규칙

          **Phase 1 (최초 실행)**:
          작업: keyword → vector_search → schedule → distance_calc
          설명: 초기 일정 생성 파이프라인

          **Phase 2 (평가 기반 개선)**:
          Optimizer의 피드백을 분석하여 적절한 전략을 선택하세요.

          - 전체 재생성이 권장되는 경우:
            작업: keyword → vector_search → schedule → distance_calc
            조건: "전체 일정 재생성을 권장합니다" 또는 대부분 장소에 문제

          - 부분 수정으로 충분한 경우:
            작업: keyword → vector_search → modify → distance_calc
            조건: 특정 장소만 문제 (1~3개 장소 교체 또는 순서 조정)

          상황을 판단하여 가장 효율적인 작업 계획을 선택하세요.

          ## 출력 형식
          - 각 작업은 정확히 순서대로 배열
          - 불필요한 작업은 제외
          """);

  public TripPlanOrchestrator(
      List<Worker<TripPlanContext, TripPlanTaskType>> workers,
      @Qualifier("openAiChatModel") ChatModel chatModel) {

    super(workers);

    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultAdvisors(
                StructuredOutputValidationAdvisor.builder()
                    .outputType(
                        new ParameterizedTypeReference<OrchestratorResponse<TripPlanTaskType>>() {})
                    .maxRepeatAttempts(3)
                    .build())
            .build();
  }

  @Override
  public List<WorkerTask<TripPlanTaskType>> planTasks(TripPlanContext context) {
    AiDailyPlanRequest request = context.getRequest();
    int dayNumber = context.getDayNumber();
    OptimizationPlan optimizationPlan = context.getOptimizationPlan();

    String phase;
    String feedback;

    if (optimizationPlan == null) {
      phase = "Phase 1: 초안 생성";
      feedback = "아직 생성된 일정이 없습니다. 전체 프로세스를 계획하세요.";
    } else {
      phase = "Phase 2: 평가 기반 개선";
      feedback =
          optimizationPlan.recommendation() != null
              ? optimizationPlan.recommendation()
              : "피드백 정보 없음";
    }

    String prompt =
        taskPlanPromptTemplate.render(
            Map.of(
                "region", request.region(),
                "dayNumber", String.valueOf(dayNumber),
                "themes",
                    request.themes().stream()
                        .map(PlaceThemeType::getDescription)
                        .collect(Collectors.joining(", ")),
                "phase", phase,
                "feedback", feedback));

    OrchestratorResponse<TripPlanTaskType> plan =
        chatClient.prompt().user(prompt).call().entity(new ParameterizedTypeReference<>() {});

    log.info("=== ORCHESTRATOR PLAN ({}) ===", phase);
    log.info("분석: {}", plan.analysis());
    plan.workerTasks().forEach(task -> log.info("  - [{}] {}", task.type(), task.description()));

    return plan.workerTasks();
  }
}
