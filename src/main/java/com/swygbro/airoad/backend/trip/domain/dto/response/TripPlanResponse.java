package com.swygbro.airoad.backend.trip.domain.dto.response;

import java.time.LocalDate;

import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;

import lombok.Builder;

@Builder
public record TripPlanResponse(
    Long id, Long memberId, String title, LocalDate startDate, String region, String imageUrl) {

  public static TripPlanResponse of(TripPlan tripPlan) {
    return TripPlanResponse.builder()
        .id(tripPlan.getId())
        .memberId(tripPlan.getMember().getId())
        .title(tripPlan.getTitle())
        .startDate(tripPlan.getStartDate())
        .region(tripPlan.getRegion())
        .imageUrl(tripPlan.getImageUrl())
        .build();
  }
}
