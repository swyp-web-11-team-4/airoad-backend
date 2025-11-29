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

    // Document 리스트 통합 (places + restaurants)
    List<Document> allDocuments = new ArrayList<>();
    allDocuments.addAll(dedupResult.places());
    allDocuments.addAll(dedupResult.restaurants());

    // placeId -> Document 매핑
    Map<Long, Document> documentMap =
        allDocuments.stream()
            .collect(
                Collectors.toMap(
                    doc -> ((Integer) doc.getMetadata().get("placeId")).longValue(),
                    doc -> doc,
                    (existing, replacement) -> existing));

    List<RouteOptimizationResponse.RoutedPlace> scheduledPlaces = routeResult.places();
    List<DistanceCalculationResponse> placesWithDistance = new ArrayList<>();

    for (int i = 0; i < scheduledPlaces.size(); i++) {
      RoutedPlace currentSchedule = scheduledPlaces.get(i);

      int estimatedMinutes = 0;

      if (i < scheduledPlaces.size() - 1) {
        RoutedPlace nextSchedule = scheduledPlaces.get(i + 1);

        Document currentDoc = documentMap.get(currentSchedule.placeId());
        Document nextDoc = documentMap.get(nextSchedule.placeId());

        if (hasCoordinates(currentDoc) && hasCoordinates(nextDoc)) {
          Double currentLat = (Double) currentDoc.getMetadata().get("latitude");
          Double currentLon = (Double) currentDoc.getMetadata().get("longitude");
          Double nextLat = (Double) nextDoc.getMetadata().get("latitude");
          Double nextLon = (Double) nextDoc.getMetadata().get("longitude");

          Distance distance =
              distanceCalculationUseCase.calculateDistance(
                  currentLat, currentLon, nextLat, nextLon);

          estimatedMinutes = distance.estimatedMinutes();

          log.debug(
              "[거리계산] {} -> {}: 직선 {}km, 예상 {}분",
              currentDoc.getMetadata().get("name"),
              nextDoc.getMetadata().get("name"),
              String.format("%.2f", distance.straightlineDistanceKm()),
              estimatedMinutes);
        } else {
          log.warn("좌표 정보 부족: {} -> {}", currentSchedule.placeId(), nextSchedule.placeId());
        }
      }

      placesWithDistance.add(
          DistanceCalculationResponse.builder()
              .placeId(currentSchedule.placeId())
              .visitOrder(currentSchedule.visitOrder())
              .category(currentSchedule.category())
              .travelTime(estimatedMinutes)
              .build());
    }

    log.info("거리 계산 완료 (총 {}개 구간)", placesWithDistance.size() - 1);

    context.put(TripPlanContextKey.DISTANCE_CALCULATED_PLACES, placesWithDistance);

    return context;
  }

  @Override
  public String getName() {
    return "DistanceCalculation";
  }

  private boolean hasCoordinates(Document doc) {
    return doc != null
        && doc.getMetadata().get("latitude") != null
        && doc.getMetadata().get("longitude") != null;
  }
}
