package com.swygbro.airoad.backend.trip.domain.dto.response;

import java.time.LocalDate;

import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;

import lombok.Builder;

@Builder
public record TripPlanResponse(
    Long id,
    Long memberId,
    Long chatRoomId,
    String title,
    LocalDate startDate,
    String region,
    String imageUrl) {

  public static TripPlanResponse of(TripPlan tripPlan, Long aiConversationId) {
    return TripPlanResponse.builder()
        .id(tripPlan.getId())
        .memberId(tripPlan.getMember().getId())
        .chatRoomId(aiConversationId)
        .title(tripPlan.getTitle())
        .startDate(tripPlan.getStartDate())
        .region(tripPlan.getRegion())
        .imageUrl(tripPlan.getImageUrl())
        .build();
  }

  public static TripPlanResponse of(TripPlan tripPlan) {
    return of(tripPlan, null);
  }
}
