package com.swygbro.airoad.backend.fixture.trip;

import java.time.LocalDate;

import com.swygbro.airoad.backend.fixture.common.LocationFixture;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.domain.entity.TripTheme;

public class TripPlanFixture {

  public static TripPlan create() {
    return TripPlan.builder()
        .member(MemberFixture.create())
        .tripTheme(TripThemeFixture.create())
        .title("서울 3박 4일 여행")
        .startDate(LocalDate.of(2025, 12, 1))
        .endDate(LocalDate.of(2025, 12, 4))
        .isCompleted(false)
        .region("서울")
        .transportation(Transportation.PUBLIC_TRANSIT)
        .budget("50만원")
        .peopleCount(2)
        .startLocation(LocationFixture.create())
        .endLocation(LocationFixture.createGangnam())
        .build();
  }

  public static TripPlan createCompleted() {
    return TripPlan.builder()
        .member(MemberFixture.create())
        .tripTheme(TripThemeFixture.create())
        .title("제주도 2박 3일 여행")
        .startDate(LocalDate.of(2025, 11, 15))
        .endDate(LocalDate.of(2025, 11, 17))
        .isCompleted(true)
        .region("제주")
        .transportation(Transportation.CAR)
        .budget("100만원")
        .peopleCount(4)
        .startLocation(LocationFixture.createJejuAirport())
        .endLocation(LocationFixture.createJejuAirport())
        .build();
  }

  public static TripPlan createWithMember(Member member) {
    return TripPlan.builder()
        .member(member)
        .tripTheme(TripThemeFixture.create())
        .title("서울 3박 4일 여행")
        .startDate(LocalDate.of(2025, 12, 1))
        .endDate(LocalDate.of(2025, 12, 4))
        .isCompleted(false)
        .region("서울")
        .transportation(Transportation.PUBLIC_TRANSIT)
        .budget("50만원")
        .peopleCount(2)
        .startLocation(LocationFixture.create())
        .endLocation(LocationFixture.createGangnam())
        .build();
  }

  public static TripPlan createWithMemberAndTheme(Member member, TripTheme tripTheme) {
    return TripPlan.builder()
        .member(member)
        .tripTheme(tripTheme)
        .title("서울 3박 4일 여행")
        .startDate(LocalDate.of(2025, 12, 1))
        .endDate(LocalDate.of(2025, 12, 4))
        .isCompleted(false)
        .region("서울")
        .transportation(Transportation.PUBLIC_TRANSIT)
        .budget("50만원")
        .peopleCount(2)
        .startLocation(LocationFixture.create())
        .endLocation(LocationFixture.createGangnam())
        .build();
  }

  public static TripPlan.TripPlanBuilder builder() {
    return TripPlan.builder()
        .member(MemberFixture.create())
        .tripTheme(TripThemeFixture.create())
        .title("테스트 여행")
        .startDate(LocalDate.now())
        .endDate(LocalDate.now().plusDays(2))
        .isCompleted(false)
        .region("서울")
        .transportation(Transportation.PUBLIC_TRANSIT)
        .budget("50만원")
        .peopleCount(2)
        .startLocation(LocationFixture.create())
        .endLocation(LocationFixture.createGangnam());
  }
}
