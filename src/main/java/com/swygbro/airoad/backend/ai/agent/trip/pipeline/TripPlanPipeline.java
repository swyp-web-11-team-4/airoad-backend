package com.swygbro.airoad.backend.ai.agent.trip.pipeline;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.trip.context.ExecutionContext;
import com.swygbro.airoad.backend.ai.agent.trip.context.TripPlanContextKey;
import com.swygbro.airoad.backend.ai.agent.trip.dto.common.ExecutionResult;
import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse;
import com.swygbro.airoad.backend.ai.agent.trip.step.DeduplicationStep;
import com.swygbro.airoad.backend.ai.agent.trip.step.DistanceCalculationStep;
import com.swygbro.airoad.backend.ai.agent.trip.step.KeywordGenerationStep;
import com.swygbro.airoad.backend.ai.agent.trip.step.PlaceSearchStep;
import com.swygbro.airoad.backend.ai.agent.trip.step.RouteOptimizationStep;
import com.swygbro.airoad.backend.ai.agent.trip.step.ScheduleCompletionStep;
import com.swygbro.airoad.backend.ai.agent.trip.step.ScheduleSummaryStep;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TripPlanPipeline {

  private final List<PipelineStep> steps;

  public TripPlanPipeline(
      KeywordGenerationStep keywordStep,
      PlaceSearchStep placeSearchStep,
      DeduplicationStep deduplicationStep,
      RouteOptimizationStep routeOptimizationStep,
      ScheduleSummaryStep scheduleSummaryStep,
      DistanceCalculationStep distanceStep,
      ScheduleCompletionStep completionStep) {

    this.steps =
        List.of(
            keywordStep,
            placeSearchStep,
            deduplicationStep,
            routeOptimizationStep,
            scheduleSummaryStep,
            distanceStep,
            completionStep);
  }

  public ExecutionResult execute(
      AiDailyPlanRequest request,
      Integer dayNumber,
      List<Long> previousPlaceIds,
      List<String> visitedDistricts) {
    log.info("여행 일정 생성 파이프라인 시작 - 지역: {}, {}일차", request.region(), dayNumber);

    ExecutionContext context = new ExecutionContext();
    context.put(TripPlanContextKey.REQUEST, request);
    context.put(TripPlanContextKey.DAY_NUMBER, dayNumber);
    context.put(
        TripPlanContextKey.PREVIOUS_PLACE_IDS,
        previousPlaceIds != null ? previousPlaceIds : new ArrayList<>());
    context.put(
        TripPlanContextKey.VISITED_DISTRICTS,
        visitedDistricts != null ? visitedDistricts : new ArrayList<>());

    for (PipelineStep step : steps) {
      context = step.execute(context);
      log.debug("파이프라인 스텝 실행 완료: {}", step.getName());
    }

    AiDailyPlanResponse plan = context.get(TripPlanContextKey.SCHEDULE);
    String selectedDistrict = context.get(TripPlanContextKey.SELECTED_DISTRICT);

    return ExecutionResult.of(plan, selectedDistrict);
  }
}
