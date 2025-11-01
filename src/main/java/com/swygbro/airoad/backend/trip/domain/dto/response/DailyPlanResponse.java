package com.swygbro.airoad.backend.trip.domain.dto.response;

import java.util.List;

import lombok.Builder;

@Builder
public record DailyPlanResponse(
    Long id,
    Integer dayNumber,
    String date,
    String title,
    String description,
    List<ScheduledPlaceResponse> scheduledPlaces) {}
