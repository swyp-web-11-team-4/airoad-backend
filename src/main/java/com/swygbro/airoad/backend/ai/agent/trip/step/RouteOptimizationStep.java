package com.swygbro.airoad.backend.ai.agent.trip.step;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.trip.context.ExecutionContext;
import com.swygbro.airoad.backend.ai.agent.trip.context.TripPlanContextKey;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.DeduplicationResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.RouteOptimizationResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.RouteOptimizationResponse.RoutedPlace;
import com.swygbro.airoad.backend.ai.agent.trip.pipeline.PipelineStep;
import com.swygbro.airoad.backend.content.application.RouteOptimizationUseCase;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RouteOptimizationStep implements PipelineStep {

  private final RouteOptimizationUseCase routeOptimizationUseCase;

  @Override
  public ExecutionContext execute(ExecutionContext context) {
    DeduplicationResponse dedupResult = context.get(TripPlanContextKey.DEDUP_RESULT);

    List<Document> route =
        routeOptimizationUseCase.optimizeRoute(dedupResult.places(), dedupResult.restaurants());

    RouteOptimizationResponse response = buildResponse(route);
    context.put(TripPlanContextKey.ROUTE_OPTIMIZATION_RESULT, response);

    return context;
  }

  @Override
  public String getName() {
    return "RouteOptimization";
  }

  private RouteOptimizationResponse buildResponse(List<Document> route) {
    List<RoutedPlace> places = new ArrayList<>();
    for (int i = 0; i < route.size(); i++) {
      Document doc = route.get(i);
      Long placeId = ((Integer) doc.getMetadata().get("placeId")).longValue();
      ScheduledCategory category = determineCategory(i);
      places.add(
          RoutedPlace.builder().placeId(placeId).visitOrder(i + 1).category(category).build());
    }
    return RouteOptimizationResponse.of(places);
  }

  private ScheduledCategory determineCategory(int index) {
    if (index == 0) return ScheduledCategory.MORNING;
    if (index == 1) return ScheduledCategory.LUNCH;
    if (index == 2) return ScheduledCategory.AFTERNOON;
    if (index == 3) return ScheduledCategory.DINNER;
    return ScheduledCategory.EVENING;
  }
}
