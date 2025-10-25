package com.swygbro.airoad.backend.fixture.trip;

import java.time.LocalDate;

import com.swygbro.airoad.backend.trip.domain.entity.DailyPlan;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;

public class DailyPlanFixture {

  public static DailyPlan create() {
    return DailyPlan.builder()
        .tripPlan(TripPlanFixture.create())
        .date(LocalDate.of(2025, 12, 1))
        .build();
  }

  public static DailyPlan createWithTripPlan(TripPlan tripPlan) {
    return DailyPlan.builder().tripPlan(tripPlan).date(LocalDate.of(2025, 12, 1)).build();
  }

  public static DailyPlan createWithTripPlanAndDate(TripPlan tripPlan, LocalDate date) {
    return DailyPlan.builder().tripPlan(tripPlan).date(date).build();
  }

  public static DailyPlan.DailyPlanBuilder builder() {
    return DailyPlan.builder().tripPlan(TripPlanFixture.create()).date(LocalDate.now());
  }
}
