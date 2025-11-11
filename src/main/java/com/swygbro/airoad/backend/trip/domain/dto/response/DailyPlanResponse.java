package com.swygbro.airoad.backend.trip.domain.dto.response;

import java.util.List;

import com.swygbro.airoad.backend.trip.domain.entity.DailyPlan;

import lombok.Builder;

@Builder
public record DailyPlanResponse(
    Long id,
    Integer dayNumber,
    String date,
    String title,
    String description,
    List<ScheduledPlaceResponse> scheduledPlaces) {

  public static DailyPlanResponse of(DailyPlan dailyPlan) {
    List<ScheduledPlaceResponse> scheduledPlaceResponses =
        dailyPlan.getScheduledPlaces().stream().map(ScheduledPlaceResponse::of).toList();

    return DailyPlanResponse.builder()
        .id(dailyPlan.getId())
        .dayNumber(dailyPlan.getDayNumber())
        .date(dailyPlan.getDate().toString())
        .title(dailyPlan.getTitle())
        .description(dailyPlan.getDescription())
        .scheduledPlaces(scheduledPlaceResponses)
        .build();
  }
}
