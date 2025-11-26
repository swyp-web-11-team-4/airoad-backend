package com.swygbro.airoad.backend.ai.agent.trip.v2.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.common.worker.Worker;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse.AiScheduledPlace;
import com.swygbro.airoad.backend.ai.agent.trip.v2.context.TripPlanContext;
import com.swygbro.airoad.backend.content.application.DistanceCalculationUseCase;
import com.swygbro.airoad.backend.content.application.PlaceQueryUseCase;
import com.swygbro.airoad.backend.content.domain.dto.response.PlaceResponse;
import com.swygbro.airoad.backend.content.domain.vo.Distance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistanceCalculationWorker implements Worker<TripPlanContext, TripPlanTaskType> {

  private final PlaceQueryUseCase placeQueryUseCase;
  private final DistanceCalculationUseCase distanceCalculationUseCase;

  @Override
  public TripPlanTaskType getTaskType() {
    return TripPlanTaskType.DISTANCE_CALC;
  }

  @Override
  public void execute(TripPlanContext context) {
    AiDailyPlanResponse currentPlan = context.getDailyPlan();

    if (currentPlan == null || currentPlan.places() == null || currentPlan.places().isEmpty()) {
      log.warn("거리 계산을 수행할 일정이 없습니다.");
      return;
    }

    List<AiScheduledPlace> scheduledPlaces = currentPlan.places();

    List<Long> placeIds = scheduledPlaces.stream().map(AiScheduledPlace::placeId).toList();

    Map<Long, PlaceResponse> placeMap =
        placeQueryUseCase.findAllPlaceById(placeIds).stream()
            .collect(Collectors.toMap(PlaceResponse::id, Function.identity()));

    List<AiScheduledPlace> updatedPlaces = new ArrayList<>();

    for (int i = 0; i < scheduledPlaces.size(); i++) {
      AiScheduledPlace currentSchedule = scheduledPlaces.get(i);

      if (i == scheduledPlaces.size() - 1) {
        updatedPlaces.add(createNewPlace(currentSchedule, 0));
        continue;
      }

      AiScheduledPlace nextSchedule = scheduledPlaces.get(i + 1);

      PlaceResponse currentPlace = placeMap.get(currentSchedule.placeId());
      PlaceResponse nextPlace = placeMap.get(nextSchedule.placeId());

      int estimatedMinutes;

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
        estimatedMinutes = currentSchedule.travelTime();
      }

      updatedPlaces.add(createNewPlace(currentSchedule, estimatedMinutes));
    }

    AiDailyPlanResponse updatedPlan =
        new AiDailyPlanResponse(
            currentPlan.dayNumber(),
            currentPlan.date(),
            currentPlan.title(),
            currentPlan.description(),
            updatedPlaces);

    context.setDailyPlan(updatedPlan);
    log.info("{}일차 거리 계산 완료 (총 {}개 구간)", context.getDayNumber(), updatedPlaces.size() - 1);
  }

  private boolean hasCoordinates(PlaceResponse place) {
    return place != null && place.latitude() != null && place.longitude() != null;
  }

  private AiScheduledPlace createNewPlace(AiScheduledPlace original, int minutes) {
    return new AiScheduledPlace(
        original.placeId(),
        original.summary(),
        original.visitOrder(),
        original.category(),
        minutes,
        original.transportation());
  }
}
