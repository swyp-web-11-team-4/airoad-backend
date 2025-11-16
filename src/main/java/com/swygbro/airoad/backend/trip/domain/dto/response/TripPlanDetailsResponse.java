package com.swygbro.airoad.backend.trip.domain.dto.response;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;

@Builder
public record TripPlanDetailsResponse(
    Long id,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    List<DailyPlanResponse> dailyPlans) {}
