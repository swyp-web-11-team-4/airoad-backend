package com.swygbro.airoad.backend.fixture.trip;

import java.time.LocalTime;

import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.fixture.common.TravelSegmentFixture;
import com.swygbro.airoad.backend.fixture.content.PlaceFixture;
import com.swygbro.airoad.backend.trip.domain.entity.DailyPlan;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledPlace;

public class ScheduledPlaceFixture {

  public static ScheduledPlace create() {
    return ScheduledPlace.builder()
        .dailyPlan(DailyPlanFixture.create())
        .place(PlaceFixture.create())
        .visitOrder(1)
        .category(ScheduledCategory.MORNING)
        .plannedTime(LocalTime.of(9, 0))
        .travelSegment(TravelSegmentFixture.create())
        .build();
  }

  public static ScheduledPlace createMorning() {
    return ScheduledPlace.builder()
        .dailyPlan(DailyPlanFixture.create())
        .place(PlaceFixture.create())
        .visitOrder(1)
        .category(ScheduledCategory.MORNING)
        .plannedTime(LocalTime.of(9, 0))
        .travelSegment(TravelSegmentFixture.createWalking())
        .build();
  }

  public static ScheduledPlace createLunch() {
    return ScheduledPlace.builder()
        .dailyPlan(DailyPlanFixture.create())
        .place(PlaceFixture.createRestaurant())
        .visitOrder(2)
        .category(ScheduledCategory.LUNCH)
        .plannedTime(LocalTime.of(12, 0))
        .travelSegment(TravelSegmentFixture.create())
        .build();
  }

  public static ScheduledPlace createAfternoon() {
    return ScheduledPlace.builder()
        .dailyPlan(DailyPlanFixture.create())
        .place(PlaceFixture.createMustVisit())
        .visitOrder(3)
        .category(ScheduledCategory.AFTERNOON)
        .plannedTime(LocalTime.of(14, 0))
        .travelSegment(TravelSegmentFixture.createByCar())
        .build();
  }

  public static ScheduledPlace createDinner() {
    return ScheduledPlace.builder()
        .dailyPlan(DailyPlanFixture.create())
        .place(PlaceFixture.createRestaurant())
        .visitOrder(4)
        .category(ScheduledCategory.DINNER)
        .plannedTime(LocalTime.of(18, 30))
        .travelSegment(TravelSegmentFixture.create())
        .build();
  }

  public static ScheduledPlace createWithDailyPlan(DailyPlan dailyPlan) {
    return ScheduledPlace.builder()
        .dailyPlan(dailyPlan)
        .place(PlaceFixture.create())
        .visitOrder(1)
        .category(ScheduledCategory.MORNING)
        .plannedTime(LocalTime.of(9, 0))
        .travelSegment(TravelSegmentFixture.create())
        .build();
  }

  public static ScheduledPlace createWithDailyPlanAndPlace(DailyPlan dailyPlan, Place place) {
    return ScheduledPlace.builder()
        .dailyPlan(dailyPlan)
        .place(place)
        .visitOrder(1)
        .category(ScheduledCategory.MORNING)
        .plannedTime(LocalTime.of(9, 0))
        .travelSegment(TravelSegmentFixture.create())
        .build();
  }

  public static ScheduledPlace.ScheduledPlaceBuilder builder() {
    return ScheduledPlace.builder()
        .dailyPlan(DailyPlanFixture.create())
        .place(PlaceFixture.create())
        .visitOrder(1)
        .category(ScheduledCategory.MORNING)
        .plannedTime(LocalTime.of(9, 0))
        .travelSegment(TravelSegmentFixture.create());
  }
}
