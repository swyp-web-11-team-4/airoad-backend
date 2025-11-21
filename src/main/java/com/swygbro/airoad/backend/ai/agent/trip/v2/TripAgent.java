package com.swygbro.airoad.backend.ai.agent.trip.v2;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.common.dto.WorkerTask;
import com.swygbro.airoad.backend.ai.agent.common.evaluator.Evaluator.EvaluationResult;
import com.swygbro.airoad.backend.ai.agent.common.optimizer.Optimizer;
import com.swygbro.airoad.backend.ai.agent.common.orchestrator.Orchestrator;
import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse.AiScheduledPlace;
import com.swygbro.airoad.backend.ai.agent.trip.v2.context.TripPlanContext;
import com.swygbro.airoad.backend.ai.agent.trip.v2.evaluator.TripPlanEvaluator;
import com.swygbro.airoad.backend.ai.agent.trip.v2.optimizer.TripPlanIssueType;
import com.swygbro.airoad.backend.ai.agent.trip.v2.worker.TripPlanTaskType;
import com.swygbro.airoad.backend.ai.common.agent.AiroadAgent;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.event.DailyPlanGeneratedEvent;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.trip.domain.dto.request.DailyPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceCreateRequest;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationCompletedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationErrorEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Primary
@Component("tripAgentV2")
@Slf4j
@RequiredArgsConstructor
public class TripAgent implements AiroadAgent {

  private final AgentType agentType = AgentType.TRIP_AGENT;
  private final ApplicationEventPublisher eventPublisher;
  private final Orchestrator<TripPlanContext, TripPlanTaskType> orchestrator;
  private final TripPlanEvaluator evaluator;
  private final Optimizer<TripPlanContext, TripPlanIssueType> optimizer;

  private static final int MAX_ATTEMPTS = 3;

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
      List<Long> allPreviousPlaceIds = new ArrayList<>();

      for (int dayNumber = 1; dayNumber <= request.duration(); dayNumber++) {
        log.debug("{}일차 일정 생성 시작", dayNumber);

        AiDailyPlanResponse dailyPlan = generateDailyPlan(request, dayNumber, allPreviousPlaceIds);

        DailyPlanCreateRequest dailyPlanCreateRequest = toDailyPlanDto(dailyPlan);
        DailyPlanGeneratedEvent event =
            DailyPlanGeneratedEvent.builder()
                .chatRoomId(request.chatRoomId())
                .tripPlanId(request.tripPlanId())
                .username(request.username())
                .dailyPlan(dailyPlanCreateRequest)
                .build();

        eventPublisher.publishEvent(event);

        List<Long> currentPlaceIds =
            dailyPlan.places().stream().map(AiScheduledPlace::placeId).toList();
        allPreviousPlaceIds.addAll(currentPlaceIds);

        log.info("{}일차 일정 생성 완료 (사용한 장소 {}개 누적)", dayNumber, allPreviousPlaceIds.size());
      }

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

  private AiDailyPlanResponse generateDailyPlan(
      AiDailyPlanRequest request, int dayNumber, List<Long> previousPlaceIds) {
    TripPlanContext context = generateInitialPlan(request, dayNumber, previousPlaceIds);
    return refineWithEvaluation(context);
  }

  private TripPlanContext generateInitialPlan(
      AiDailyPlanRequest request, int dayNumber, List<Long> previousPlaceIds) {
    TripPlanContext context = TripPlanContext.of(request, dayNumber, previousPlaceIds);
    List<WorkerTask<TripPlanTaskType>> tasks = orchestrator.planTasks(context);
    return orchestrator.executeTasks(tasks, context);
  }

  private AiDailyPlanResponse refineWithEvaluation(TripPlanContext context) {
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      log.info("\n===== 시도 {}/{} =====", attempt, MAX_ATTEMPTS);

      EvaluationResult<TripPlanIssueType> evaluation = evaluator.evaluate(context);
      context.setEvaluationResult(evaluation);

      if (evaluation.evaluation() == EvaluationResult.Evaluation.PASS) {
        log.info("평가 통과");
        return context.getDailyPlan();
      }

      log.info("평가 실패");
      Optimizer.OptimizationPlan optimization = optimizer.optimize(evaluation, context);
      context.setOptimizationPlan(optimization);

      List<WorkerTask<TripPlanTaskType>> tasks = orchestrator.planTasks(context);
      context = orchestrator.executeTasks(tasks, context);
    }

    log.warn("최대 시도 횟수 도달, 마지막 결과 반환");
    return context.getDailyPlan();
  }

  private DailyPlanCreateRequest toDailyPlanDto(AiDailyPlanResponse aiDailyPlanResponse) {
    List<ScheduledPlaceCreateRequest> scheduledPlaces =
        aiDailyPlanResponse.places().stream()
            .map(
                p ->
                    ScheduledPlaceCreateRequest.builder()
                        .placeId(p.placeId())
                        .category(p.category())
                        .travelTime(p.travelTime())
                        .transportation(p.transportation())
                        .visitOrder(p.visitOrder())
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
