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
import com.swygbro.airoad.backend.content.application.PlaceQueryUseCase;
import com.swygbro.airoad.backend.content.domain.dto.response.PlaceResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistanceCalculationWorker implements Worker<TripPlanContext, TripPlanTaskType> {

  private final PlaceQueryUseCase placeQueryUseCase;

  private static final double TORTUOSITY_FACTOR = 1.8;

  private static final double AVERAGE_SPEED_KMPH = 25.0;

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
        updatedPlaces.add(createNewPlaceWithTime(currentSchedule, 0));
        continue;
      }

      AiScheduledPlace nextSchedule = scheduledPlaces.get(i + 1);

      PlaceResponse currentPlace = placeMap.get(currentSchedule.placeId());
      PlaceResponse nextPlace = placeMap.get(nextSchedule.placeId());

      int estimatedMinutes;

      if (hasCoordinates(currentPlace) && hasCoordinates(nextPlace)) {
        double straightDistanceKm =
            calculateHaversine(
                currentPlace.latitude(), currentPlace.longitude(),
                nextPlace.latitude(), nextPlace.longitude());

        estimatedMinutes = calculateEstimatedMinutes(straightDistanceKm);

        log.debug(
            "[거리계산] {} -> {}: 직선 {}km, 예상 {}분",
            currentPlace.name(),
            nextPlace.name(),
            straightDistanceKm,
            estimatedMinutes);
      } else {
        log.warn("좌표 정보 부족: {} -> {}", currentSchedule.placeId(), nextSchedule.placeId());
        estimatedMinutes = currentSchedule.travelTime();
      }

      updatedPlaces.add(createNewPlaceWithTime(currentSchedule, estimatedMinutes));
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

  private AiScheduledPlace createNewPlaceWithTime(AiScheduledPlace original, int minutes) {
    return new AiScheduledPlace(
        original.placeId(),
        original.visitOrder(),
        original.category(),
        minutes,
        original.transportation());
  }

  private double calculateHaversine(double lat1, double lon1, double lat2, double lon2) {
    final int R = 6371;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  private int calculateEstimatedMinutes(double straightDistanceKm) {
    double roadDistance = straightDistanceKm * TORTUOSITY_FACTOR;
    double hours = roadDistance / AVERAGE_SPEED_KMPH;
    return (int) Math.round(hours * 60);
  }
}
