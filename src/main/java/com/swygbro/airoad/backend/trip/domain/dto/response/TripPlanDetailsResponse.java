package com.swygbro.airoad.backend.trip.domain.dto.response;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TripPlanDetailsResponse {
  private final Long id;
  private final String title;
  private final LocalDate startDate;
  private final LocalDate endDate;
  private final List<DailyPlanResponse> dailyPlans;
}
