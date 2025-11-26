package com.swygbro.airoad.backend.ai.agent.trip.v3.step;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.trip.v3.context.ExecutionContext;
import com.swygbro.airoad.backend.ai.agent.trip.v3.context.TripPlanContextKey;
import com.swygbro.airoad.backend.ai.agent.trip.v3.dto.DistanceCalculationResponse;
import com.swygbro.airoad.backend.ai.agent.trip.v3.dto.ScheduleCreationResponse;
import com.swygbro.airoad.backend.ai.agent.trip.v3.dto.ScheduleCreationResponse.ScheduledPlaceInfo;
import com.swygbro.airoad.backend.ai.agent.trip.v3.pipeline.PipelineStep;
import com.swygbro.airoad.backend.content.application.DistanceCalculationUseCase;
import com.swygbro.airoad.backend.content.application.PlaceQueryUseCase;
import com.swygbro.airoad.backend.content.domain.dto.response.PlaceResponse;
import com.swygbro.airoad.backend.content.domain.vo.Distance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistanceCalculationStep implements PipelineStep {

  private final PlaceQueryUseCase placeQueryUseCase;
  private final DistanceCalculationUseCase distanceCalculationUseCase;

  @Override
  public ExecutionContext execute(ExecutionContext context) {
    ScheduleCreationResponse scheduleCreationResult =
        context.get(TripPlanContextKey.SCHEDULE_CREATION_RESULT);

    if (scheduleCreationResult == null
        || scheduleCreationResult.places() == null
        || scheduleCreationResult.places().isEmpty()) {
      log.warn("거리 계산을 수행할 일정이 없습니다.");
      return context;
    }

    List<ScheduledPlaceInfo> scheduledPlaces = scheduleCreationResult.places();

    List<Long> placeIds = scheduledPlaces.stream().map(ScheduledPlaceInfo::placeId).toList();

    Map<Long, PlaceResponse> placeMap =
        placeQueryUseCase.findAllPlaceById(placeIds).stream()
            .collect(Collectors.toMap(PlaceResponse::id, Function.identity()));

    List<DistanceCalculationResponse> placesWithDistance = new ArrayList<>();

    for (int i = 0; i < scheduledPlaces.size(); i++) {
      ScheduledPlaceInfo currentSchedule = scheduledPlaces.get(i);

      int estimatedMinutes = 0;

      if (i < scheduledPlaces.size() - 1) {
        ScheduledPlaceInfo nextSchedule = scheduledPlaces.get(i + 1);

        PlaceResponse currentPlace = placeMap.get(currentSchedule.placeId());
        PlaceResponse nextPlace = placeMap.get(nextSchedule.placeId());

        if (hasCoordinates(currentPlace) && hasCoordinates(nextPlace)) {
          Distance distance =
              distanceCalculationUseCase.calculateDistance(
                  currentPlace.latitude(),
                  currentPlace.longitude(),
                  nextPlace.latitude(),
                  nextPlace.longitude());

          estimatedMinutes = distance.estimatedMinutes();

          log.debug(
              "[거리계산] {} -> {}: 직선 {}km, 예상 {}분",
              currentPlace.name(),
              nextPlace.name(),
              distance.straightlineDistanceKm(),
              estimatedMinutes);
        } else {
          log.warn("좌표 정보 부족: {} -> {}", currentSchedule.placeId(), nextSchedule.placeId());
        }
      }

      placesWithDistance.add(DistanceCalculationResponse.of(currentSchedule, estimatedMinutes));
    }

    log.info("거리 계산 완료 (총 {}개 구간)", placesWithDistance.size() - 1);

    context.put(TripPlanContextKey.DISTANCE_CALCULATED_PLACES, placesWithDistance);

    return context;
  }

  @Override
  public String getName() {
    return "DistanceCalculation";
  }

  private boolean hasCoordinates(PlaceResponse place) {
    return place != null && place.latitude() != null && place.longitude() != null;
  }
}
