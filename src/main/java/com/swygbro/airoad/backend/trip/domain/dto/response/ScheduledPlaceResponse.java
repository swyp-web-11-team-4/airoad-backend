package com.swygbro.airoad.backend.trip.domain.dto.response;

import com.swygbro.airoad.backend.content.domain.dto.response.PlaceResponse;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledPlace;

import lombok.Builder;

@Builder
public record ScheduledPlaceResponse(
    Long id,
    Integer visitOrder,
    String category,
    String startTime,
    String endTime,
    Integer travelTime,
    String transportation,
    PlaceResponse place) {
  public static ScheduledPlaceResponse of(ScheduledPlace scheduledPlace) {
    return ScheduledPlaceResponse.builder()
        .id(scheduledPlace.getId())
        .visitOrder(scheduledPlace.getVisitOrder())
        .category(scheduledPlace.getCategory().name())
        .startTime(scheduledPlace.getStartTime().toString())
        .endTime(scheduledPlace.getEndTime().toString())
        .travelTime(scheduledPlace.getTravelSegment().getTravelTime())
        .transportation(scheduledPlace.getTravelSegment().getTransportation().name())
        .place(PlaceResponse.of(scheduledPlace.getPlace()))
        .build();
  }
}
