package com.swygbro.airoad.backend.ai.agent.trip.step;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.trip.context.ExecutionContext;
import com.swygbro.airoad.backend.ai.agent.trip.context.TripPlanContextKey;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.DeduplicationResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.DistanceCalculationResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.RouteOptimizationResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.RouteOptimizationResponse.RoutedPlace;
import com.swygbro.airoad.backend.ai.agent.trip.pipeline.PipelineStep;
import com.swygbro.airoad.backend.content.application.DistanceCalculationUseCase;
import com.swygbro.airoad.backend.content.domain.vo.Coordinate;
import com.swygbro.airoad.backend.content.domain.vo.Distance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistanceCalculationStep implements PipelineStep {

  private final DistanceCalculationUseCase distanceCalculationUseCase;

  @Override
  public ExecutionContext execute(ExecutionContext context) {
    RouteOptimizationResponse routeResult =
        context.get(TripPlanContextKey.ROUTE_OPTIMIZATION_RESULT);
    DeduplicationResponse dedupResult = context.get(TripPlanContextKey.DEDUP_RESULT);

    if (routeResult == null || routeResult.places() == null || routeResult.places().isEmpty()) {
      log.warn("거리 계산을 수행할 일정이 없습니다.");
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

    List<RoutedPlace> scheduledPlaces = routeResult.places();

    List<Coordinate> coordinates =
        scheduledPlaces.stream()
            .map(p -> documentMap.get(p.placeId()))
            .map(this::extractCoordinate)
            .toList();

    List<Distance> distances = distanceCalculationUseCase.calculateRouteDistances(coordinates);

    List<DistanceCalculationResponse> placesWithDistance = new ArrayList<>();

    if (!scheduledPlaces.isEmpty()) {
      RoutedPlace first = scheduledPlaces.get(0);
      placesWithDistance.add(mapToResponse(first, 0));
    }

    for (int i = 0; i < distances.size(); i++) {
      RoutedPlace currentPlace = scheduledPlaces.get(i + 1);
      Distance dist = distances.get(i);
      placesWithDistance.add(mapToResponse(currentPlace, dist.estimatedMinutes()));

      log.debug(
          "[거리계산] 구간 {}: 직선 {}km, 예상 {}분",
          i + 1,
          String.format("%.2f", dist.straightlineDistanceKm()),
          dist.estimatedMinutes());
    }

    log.info("거리 계산 완료 (총 {}개 구간)", placesWithDistance.size() - 1);

    context.put(TripPlanContextKey.DISTANCE_CALCULATED_PLACES, placesWithDistance);

    return context;
  }

  @Override
  public String getName() {
    return "DistanceCalculation";
  }

  private Coordinate extractCoordinate(Document doc) {
    if (doc == null) return null;
    Double lat = (Double) doc.getMetadata().get("latitude");
    Double lon = (Double) doc.getMetadata().get("longitude");
    return new Coordinate(lat, lon);
  }

  private DistanceCalculationResponse mapToResponse(RoutedPlace place, int travelTime) {
    return DistanceCalculationResponse.builder()
        .placeId(place.placeId())
        .visitOrder(place.visitOrder())
        .category(place.category())
        .travelTime(travelTime)
        .build();
  }
}
