package com.swygbro.airoad.backend.ai.agent.trip.v3.step;

import java.time.LocalDate;
import java.util.List;

import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse.AiScheduledPlace;
import com.swygbro.airoad.backend.ai.agent.trip.v3.context.ExecutionContext;
import com.swygbro.airoad.backend.ai.agent.trip.v3.context.TripPlanContextKey;
import com.swygbro.airoad.backend.ai.agent.trip.v3.dto.ScheduleCreationResponse;
import com.swygbro.airoad.backend.ai.agent.trip.v3.dto.DistanceCalculationResponse;
import com.swygbro.airoad.backend.ai.agent.trip.v3.pipeline.PipelineStep;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@org.springframework.stereotype.Component
public class ScheduleCompletionStep implements PipelineStep {

  @Override
  public ExecutionContext execute(ExecutionContext context) {
    ScheduleCreationResponse scheduleCreationResult =
        context.get(TripPlanContextKey.SCHEDULE_CREATION_RESULT);
    AiDailyPlanRequest request = context.get(TripPlanContextKey.REQUEST);
    Integer dayNumber = context.get(TripPlanContextKey.DAY_NUMBER);
    List<DistanceCalculationResponse> distanceCalculatedPlaces =
        context.get(TripPlanContextKey.DISTANCE_CALCULATED_PLACES);

    if (distanceCalculatedPlaces == null
        || distanceCalculatedPlaces.isEmpty()
        || scheduleCreationResult == null) {
      log.warn("일정 완성을 수행할 거리 계산 결과 또는 일정 생성 결과가 없습니다.");
      return context;
    }

    List<AiScheduledPlace> aiScheduledPlaces =
        distanceCalculatedPlaces.stream()
            .map(
                p ->
                    AiScheduledPlace.builder()
                        .placeId(p.placeId())
                        .summary(p.summary())
                        .visitOrder(p.visitOrder())
                        .category(p.category())
                        .travelTime(p.travelTime())
                        .transportation(Transportation.PUBLIC_TRANSIT)
                        .build())
            .toList();

    LocalDate scheduleDate = request.startDate().plusDays(dayNumber - 1);

    AiDailyPlanResponse dailyPlan =
        AiDailyPlanResponse.builder()
            .dayNumber(dayNumber)
            .date(scheduleDate)
            .title(scheduleCreationResult.title())
            .description(scheduleCreationResult.description())
            .places(aiScheduledPlaces)
            .build();

    log.info("일정 완성 완료: {}일차, {}개 장소", dayNumber, aiScheduledPlaces.size());

    context.put(TripPlanContextKey.SCHEDULE, dailyPlan);

    return context;
  }

  @Override
  public String getName() {
    return "ScheduleCompletion";
  }
}
