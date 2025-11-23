package com.swygbro.airoad.backend.fixture.trip;

import java.lang.reflect.Field;
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
        .startTime(LocalTime.of(9, 0))
        .endTime(LocalTime.of(11, 0))
        .travelSegment(TravelSegmentFixture.create())
        .build();
  }

  public static ScheduledPlace createMorning() {
    return ScheduledPlace.builder()
        .dailyPlan(DailyPlanFixture.create())
        .place(PlaceFixture.create())
        .visitOrder(1)
        .category(ScheduledCategory.MORNING)
        .startTime(LocalTime.of(9, 0))
        .endTime(LocalTime.of(11, 0))
        .travelSegment(TravelSegmentFixture.createWalking())
        .build();
  }

  public static ScheduledPlace createAfternoon() {
    return ScheduledPlace.builder()
        .dailyPlan(DailyPlanFixture.create())
        .place(PlaceFixture.createMustVisit())
        .visitOrder(3)
        .category(ScheduledCategory.AFTERNOON)
        .startTime(LocalTime.of(14, 0))
        .endTime(LocalTime.of(16, 0))
        .travelSegment(TravelSegmentFixture.createByCar())
        .build();
  }

  public static ScheduledPlace createWithDailyPlan(DailyPlan dailyPlan) {
    return ScheduledPlace.builder()
        .dailyPlan(dailyPlan)
        .place(PlaceFixture.create())
        .visitOrder(1)
        .category(ScheduledCategory.MORNING)
        .startTime(LocalTime.of(9, 0))
        .endTime(LocalTime.of(11, 0))
        .travelSegment(TravelSegmentFixture.create())
        .build();
  }

  public static ScheduledPlace createWithDailyPlanAndPlace(DailyPlan dailyPlan, Place place) {
    return ScheduledPlace.builder()
        .dailyPlan(dailyPlan)
        .place(place)
        .visitOrder(1)
        .category(ScheduledCategory.MORNING)
        .startTime(LocalTime.of(9, 0))
        .endTime(LocalTime.of(11, 0))
        .travelSegment(TravelSegmentFixture.create())
        .build();
  }

  public static ScheduledPlace.ScheduledPlaceBuilder builder() {
    return ScheduledPlace.builder()
        .dailyPlan(DailyPlanFixture.create())
        .place(PlaceFixture.create())
        .visitOrder(1)
        .category(ScheduledCategory.MORNING)
        .startTime(LocalTime.of(9, 0))
        .endTime(LocalTime.of(11, 0))
        .travelSegment(TravelSegmentFixture.create());
  }

  /**
   * ID가 설정된 ScheduledPlace 생성 (Reflection 사용)
   *
   * <p>JPA가 자동 생성하는 ID를 테스트에서 설정하기 위해 Reflection을 사용합니다.
   *
   * @param id 설정할 ID
   * @param scheduledPlace ID를 설정할 ScheduledPlace 객체
   * @return ID가 설정된 ScheduledPlace 객체
   */
  public static ScheduledPlace withId(Long id, ScheduledPlace scheduledPlace) {
    try {
      Field idField = scheduledPlace.getClass().getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(scheduledPlace, id);
      return scheduledPlace;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("ID 설정 실패", e);
    }
  }
}
