package com.swygbro.airoad.backend.ai.agent.trip.v3.dto;

import com.swygbro.airoad.backend.ai.agent.trip.v3.dto.ScheduleCreationResponse.ScheduledPlaceInfo;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;

import lombok.Builder;

@Builder
public record DistanceCalculationResponse(
    Long placeId, String summary, int visitOrder, ScheduledCategory category, int travelTime) {

  public static DistanceCalculationResponse of(ScheduledPlaceInfo info, int travelTime) {
    return DistanceCalculationResponse.builder()
        .placeId(info.placeId())
        .summary(info.summary())
        .visitOrder(info.visitOrder())
        .category(info.category())
        .travelTime(travelTime)
        .build();
  }
}
