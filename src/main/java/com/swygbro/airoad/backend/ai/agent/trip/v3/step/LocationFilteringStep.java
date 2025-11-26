package com.swygbro.airoad.backend.ai.agent.trip.v3.step;

import java.util.Comparator;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.trip.v3.context.ExecutionContext;
import com.swygbro.airoad.backend.ai.agent.trip.v3.context.TripPlanContextKey;
import com.swygbro.airoad.backend.ai.agent.trip.v3.dto.DeduplicationResponse;
import com.swygbro.airoad.backend.ai.agent.trip.v3.pipeline.PipelineStep;
import com.swygbro.airoad.backend.content.application.DistanceCalculationUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 파이프라인 Step 3.5: 앵커 기준 거리 필터링
 *
 * <p>유사도가 가장 높은 장소/음식점을 앵커로 선정하고, 해당 앵커로부터 반경 내의 장소만 필터링합니다. 이를 통해 같은 구역 내에서도 지리적으로 떨어진 장소들을 제외하여
 * 효율적인 동선을 보장합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationFilteringStep implements PipelineStep {

  private static final double RADIUS_KM = 15.0;

  private final DistanceCalculationUseCase distanceCalculationUseCase;

  @Override
  public ExecutionContext execute(ExecutionContext context) {
    DeduplicationResponse dedupResult = context.get(TripPlanContextKey.DEDUP_RESULT);

    List<Document> places = dedupResult.places();
    List<Document> restaurants = dedupResult.restaurants();

    Document placeAnchor =
        places.stream().max(Comparator.comparingDouble(Document::getScore)).orElse(null);

    Document restaurantAnchor =
        restaurants.stream().max(Comparator.comparingDouble(Document::getScore)).orElse(null);

    List<Document> filteredPlaces = filterByDistance(places, placeAnchor);
    List<Document> filteredRestaurants = filterByDistance(restaurants, restaurantAnchor);

    log.info(
        "위치 기반 필터링 완료: {}개 → {}개 장소, {}개 → {}개 음식점",
        places.size(),
        filteredPlaces.size(),
        restaurants.size(),
        filteredRestaurants.size());

    if (placeAnchor != null) {
      log.info(
          "장소 앵커: {} (유사도: {})", placeAnchor.getMetadata().get("name"), placeAnchor.getScore());
    }

    if (restaurantAnchor != null) {
      log.info(
          "음식점 앵커: {} (유사도: {})",
          restaurantAnchor.getMetadata().get("name"),
          restaurantAnchor.getScore());
    }

    DeduplicationResponse filteredResult =
        DeduplicationResponse.of(filteredPlaces, filteredRestaurants);
    context.put(TripPlanContextKey.DEDUP_RESULT, filteredResult);

    return context;
  }

  @Override
  public String getName() {
    return "LocationFiltering";
  }

  private List<Document> filterByDistance(List<Document> documents, Document anchor) {
    if (anchor == null || documents == null || documents.isEmpty()) {
      return documents;
    }

    Double anchorLat = (Double) anchor.getMetadata().get("latitude");
    Double anchorLon = (Double) anchor.getMetadata().get("longitude");

    if (anchorLat == null || anchorLon == null) {
      log.warn("앵커 장소의 좌표 정보가 없습니다");
      return documents;
    }

    return documents.stream()
        .filter(
            doc -> {
              Double lat = (Double) doc.getMetadata().get("latitude");
              Double lon = (Double) doc.getMetadata().get("longitude");

              if (lat == null || lon == null) {
                return true;
              }

              double distance =
                  distanceCalculationUseCase
                      .calculateDistance(anchorLat, anchorLon, lat, lon)
                      .straightlineDistanceKm();

              return distance <= RADIUS_KM;
            })
        .toList();
  }
}
