package com.swygbro.airoad.backend.ai.agent.trip.v2.context;

import java.util.ArrayList;
import java.util.List;

import com.swygbro.airoad.backend.ai.agent.common.context.WorkerContext;
import com.swygbro.airoad.backend.ai.agent.common.evaluator.Evaluator.EvaluationResult;
import com.swygbro.airoad.backend.ai.agent.common.optimizer.Optimizer.OptimizationPlan;
import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.KeywordResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.PlaceSearchResponse;
import com.swygbro.airoad.backend.ai.agent.trip.v2.optimizer.TripPlanIssueType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripPlanContext extends WorkerContext {

  private final AiDailyPlanRequest request;
  private final int dayNumber;

  private KeywordResponse keywords;
  private PlaceSearchResponse searchResults;
  private AiDailyPlanResponse dailyPlan;
  private EvaluationResult<TripPlanIssueType> evaluationResult;
  private OptimizationPlan optimizationPlan;

  private List<Long> previousPlaceIds;

  private TripPlanContext(AiDailyPlanRequest request, int dayNumber, List<Long> previousPlaceIds) {
    this.request = request;
    this.dayNumber = dayNumber;
    this.previousPlaceIds =
        previousPlaceIds != null ? new ArrayList<>(previousPlaceIds) : new ArrayList<>();
  }

  public static TripPlanContext of(AiDailyPlanRequest request, int dayNumber) {
    return new TripPlanContext(request, dayNumber, new ArrayList<>());
  }

  public static TripPlanContext of(
      AiDailyPlanRequest request, int dayNumber, List<Long> previousPlaceIds) {
    return new TripPlanContext(request, dayNumber, previousPlaceIds);
  }
}
