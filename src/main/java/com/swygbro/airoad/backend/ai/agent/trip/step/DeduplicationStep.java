package com.swygbro.airoad.backend.ai.agent.trip.step;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.trip.context.ExecutionContext;
import com.swygbro.airoad.backend.ai.agent.trip.context.TripPlanContextKey;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.DeduplicationResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.PlaceSearchResponse;
import com.swygbro.airoad.backend.ai.agent.trip.pipeline.PipelineStep;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DeduplicationStep implements PipelineStep {

  @Override
  public ExecutionContext execute(ExecutionContext context) {
    PlaceSearchResponse searchResult = context.get(TripPlanContextKey.SEARCH_RESULTS);
    List<Long> previousPlaceIds = context.get(TripPlanContextKey.PREVIOUS_PLACE_IDS);

    List<Long> finalPreviousPlaceIds = previousPlaceIds != null ? previousPlaceIds : List.of();

    List<Document> deduplicatedPlaces =
        searchResult.places().stream()
            .filter(
                place -> {
                  Long placeId = ((Integer) place.getMetadata().get("placeId")).longValue();
                  return !finalPreviousPlaceIds.contains(placeId);
                })
            .toList();

    List<Document> deduplicatedRestaurants =
        searchResult.restaurants().stream()
            .filter(
                restaurant -> {
                  Long placeId = ((Integer) restaurant.getMetadata().get("placeId")).longValue();
                  return !finalPreviousPlaceIds.contains(placeId);
                })
            .toList();

    log.info(
        "중복 제거 완료: {}개 장소, {}개 음식점", deduplicatedPlaces.size(), deduplicatedRestaurants.size());

    DeduplicationResponse dedupResult =
        DeduplicationResponse.of(deduplicatedPlaces, deduplicatedRestaurants);
    context.put(TripPlanContextKey.DEDUP_RESULT, dedupResult);

    return context;
  }

  @Override
  public String getName() {
    return "Deduplication";
  }
}
