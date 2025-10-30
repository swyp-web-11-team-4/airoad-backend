package com.swygbro.airoad.backend.trip.domain.dto.response;

import lombok.Builder;

@Builder
public record ScheduledPlaceResponse(
    Long id,
    Long placeId,
    Integer visitOrder,
    String category,
    String startTime,
    String endTime,
    Integer travelTime,
    String transportation
) {

}
