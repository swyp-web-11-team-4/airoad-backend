package com.swygbro.airoad.backend.fixture.trip;

import java.lang.reflect.Field;
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

  /**
   * ID가 설정된 DailyPlan 생성 (Reflection 사용)
   *
   * <p>JPA가 자동 생성하는 ID를 테스트에서 설정하기 위해 Reflection을 사용합니다.
   *
   * @param id 설정할 ID
   * @param dailyPlan ID를 설정할 DailyPlan 객체
   * @return ID가 설정된 DailyPlan 객체
   */
  public static DailyPlan withId(Long id, DailyPlan dailyPlan) {
    try {
      Field idField = dailyPlan.getClass().getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(dailyPlan, id);
      return dailyPlan;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("ID 설정 실패", e);
    }
  }
}
