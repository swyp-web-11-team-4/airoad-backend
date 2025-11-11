package com.swygbro.airoad.backend.trip.infrastructure;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.fixture.content.PlaceFixture;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.fixture.trip.ScheduledPlaceFixture;
import com.swygbro.airoad.backend.fixture.trip.TripPlanFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;
import com.swygbro.airoad.backend.trip.domain.entity.DailyPlan;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledPlace;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DailyPlanRepository 테스트
 *
 * <p>Repository 계층 테스트로 실제 데이터베이스와의 상호작용을 검증합니다.
 */
@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class DailyPlanRepositoryTest {

  @Autowired private DailyPlanRepository dailyPlanRepository;

  @Autowired private TripPlanRepository tripPlanRepository;

  @Autowired private MemberRepository memberRepository;

  @Autowired private PlaceRepository placeRepository;

  @Nested
  @DisplayName("findAllByTripPlanId 메서드는")
  class FindAllByTripPlanId {

    @Test
    @DisplayName("tripPlanId로 모든 DailyPlan을 일차 순서대로 조회한다")
    void shouldReturnAllDailyPlansOrderedByDayNumber() {
      // given
      Member member = memberRepository.save(MemberFixture.create());
      TripPlan tripPlan = tripPlanRepository.save(TripPlanFixture.createWithMember(member));

      DailyPlan dailyPlan1 =
          DailyPlan.builder()
              .tripPlan(tripPlan)
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .build();
      DailyPlan dailyPlan3 =
          DailyPlan.builder()
              .tripPlan(tripPlan)
              .dayNumber(3)
              .date(LocalDate.of(2025, 12, 3))
              .build();
      DailyPlan dailyPlan2 =
          DailyPlan.builder()
              .tripPlan(tripPlan)
              .dayNumber(2)
              .date(LocalDate.of(2025, 12, 2))
              .build();

      // 순서를 뒤섞어 저장
      dailyPlanRepository.save(dailyPlan1);
      dailyPlanRepository.save(dailyPlan3);
      dailyPlanRepository.save(dailyPlan2);

      // when
      List<DailyPlan> result = dailyPlanRepository.findAllByTripPlanId(tripPlan.getId());

      // then
      assertThat(result).hasSize(3);
      assertThat(result.get(0).getDayNumber()).isEqualTo(1);
      assertThat(result.get(1).getDayNumber()).isEqualTo(2);
      assertThat(result.get(2).getDayNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("DailyPlan과 연관된 ScheduledPlace를 함께 조회한다 (JOIN FETCH)")
    void shouldFetchScheduledPlacesWithDailyPlans() {
      // given
      Member member = memberRepository.save(MemberFixture.create());
      TripPlan tripPlan = TripPlanFixture.createWithMember(member);
      Place place1 = placeRepository.save(PlaceFixture.create());
      Place place2 = placeRepository.save(PlaceFixture.createGangnam());

      DailyPlan dailyPlan =
          DailyPlan.builder()
              .tripPlan(tripPlan)
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .build();

      // ScheduledPlace 추가
      ScheduledPlace scheduledPlace1 =
          ScheduledPlaceFixture.builder().dailyPlan(dailyPlan).place(place1).visitOrder(1).build();
      ScheduledPlace scheduledPlace2 =
          ScheduledPlaceFixture.builder().dailyPlan(dailyPlan).place(place2).visitOrder(2).build();

      dailyPlan.addScheduledPlace(scheduledPlace1);
      dailyPlan.addScheduledPlace(scheduledPlace2);
      tripPlan.addDailyPlan(dailyPlan);
      tripPlanRepository.save(tripPlan);

      // when
      List<DailyPlan> result = dailyPlanRepository.findAllByTripPlanId(tripPlan.getId());

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getScheduledPlaces()).hasSize(2);
      assertThat(result.get(0).getScheduledPlaces().get(0).getVisitOrder()).isEqualTo(1);
      assertThat(result.get(0).getScheduledPlaces().get(1).getVisitOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("DailyPlan이 없는 TripPlan을 조회하면 빈 리스트를 반환한다")
    void shouldReturnEmptyListWhenNoDailyPlans() {
      // given
      Member member = memberRepository.save(MemberFixture.create());
      TripPlan tripPlan = tripPlanRepository.save(TripPlanFixture.createWithMember(member));

      // when
      List<DailyPlan> result = dailyPlanRepository.findAllByTripPlanId(tripPlan.getId());

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("특정 TripPlan의 DailyPlan만 조회한다")
    void shouldReturnDailyPlansOnlyForSpecificTripPlan() {
      // given
      Member member = memberRepository.save(MemberFixture.create());
      TripPlan tripPlan1 = TripPlanFixture.createWithMember(member);
      TripPlan tripPlan2 = TripPlanFixture.createWithMember(member);

      DailyPlan dailyPlan1 =
          DailyPlan.builder()
              .tripPlan(tripPlan1)
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .build();
      DailyPlan dailyPlan2 =
          DailyPlan.builder()
              .tripPlan(tripPlan1)
              .dayNumber(2)
              .date(LocalDate.of(2025, 12, 2))
              .build();
      DailyPlan dailyPlan3 =
          DailyPlan.builder()
              .tripPlan(tripPlan2)
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .build();

      tripPlan1.addDailyPlan(dailyPlan1);
      tripPlan1.addDailyPlan(dailyPlan2);
      tripPlan2.addDailyPlan(dailyPlan3);
      tripPlanRepository.save(tripPlan1);
      tripPlanRepository.save(tripPlan2);

      // when
      List<DailyPlan> result = dailyPlanRepository.findAllByTripPlanId(tripPlan1.getId());

      // then
      assertThat(result).hasSize(2);
      assertThat(result)
          .allMatch(dailyPlan -> dailyPlan.getTripPlan().getId().equals(tripPlan1.getId()));
    }

    @Test
    @DisplayName("여러 ScheduledPlace가 있는 DailyPlan을 조회한다")
    void shouldReturnDailyPlanWithMultipleScheduledPlaces() {
      // given
      Member member = memberRepository.save(MemberFixture.create());
      TripPlan tripPlan = TripPlanFixture.createWithMember(member);

      DailyPlan dailyPlan =
          DailyPlan.builder()
              .tripPlan(tripPlan)
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .build();

      tripPlan.addDailyPlan(dailyPlan);

      // 5개의 다른 Place 저장 후 ScheduledPlace 추가
      Place place1 = placeRepository.save(PlaceFixture.create());
      Place place2 = placeRepository.save(PlaceFixture.createGangnam());
      Place place3 = placeRepository.save(PlaceFixture.createRestaurant());
      Place place4 = placeRepository.save(PlaceFixture.createMustVisit());
      Place place5 = placeRepository.save(PlaceFixture.createJejuAirport());

      dailyPlan.addScheduledPlace(
          ScheduledPlaceFixture.builder().dailyPlan(dailyPlan).place(place1).visitOrder(1).build());
      dailyPlan.addScheduledPlace(
          ScheduledPlaceFixture.builder().dailyPlan(dailyPlan).place(place2).visitOrder(2).build());
      dailyPlan.addScheduledPlace(
          ScheduledPlaceFixture.builder().dailyPlan(dailyPlan).place(place3).visitOrder(3).build());
      dailyPlan.addScheduledPlace(
          ScheduledPlaceFixture.builder().dailyPlan(dailyPlan).place(place4).visitOrder(4).build());
      dailyPlan.addScheduledPlace(
          ScheduledPlaceFixture.builder().dailyPlan(dailyPlan).place(place5).visitOrder(5).build());

      tripPlanRepository.save(tripPlan);

      // when
      List<DailyPlan> result = dailyPlanRepository.findAllByTripPlanId(tripPlan.getId());

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getScheduledPlaces()).hasSize(5);
    }

    @Test
    @DisplayName("존재하지 않는 tripPlanId로 조회하면 빈 리스트를 반환한다")
    void shouldReturnEmptyListForNonExistentTripPlanId() {
      // given
      Long nonExistentTripPlanId = 999L;

      // when
      List<DailyPlan> result = dailyPlanRepository.findAllByTripPlanId(nonExistentTripPlanId);

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("deleteByTripPlanId 메서드는")
  class DeleteByTripPlanId {

    @Test
    @DisplayName("tripPlanId에 해당하는 모든 DailyPlan을 삭제한다")
    void shouldDeleteAllDailyPlansByTripPlanId() {
      // given
      Member member = memberRepository.save(MemberFixture.create());
      TripPlan tripPlan = TripPlanFixture.createWithMember(member);

      DailyPlan dailyPlan1 =
          DailyPlan.builder()
              .tripPlan(tripPlan)
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .build();
      DailyPlan dailyPlan2 =
          DailyPlan.builder()
              .tripPlan(tripPlan)
              .dayNumber(2)
              .date(LocalDate.of(2025, 12, 2))
              .build();

      tripPlan.addDailyPlan(dailyPlan1);
      tripPlan.addDailyPlan(dailyPlan2);
      tripPlanRepository.save(tripPlan);

      // when
      dailyPlanRepository.deleteByTripPlanId(tripPlan.getId());

      // then
      List<DailyPlan> result = dailyPlanRepository.findAllByTripPlanId(tripPlan.getId());
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("특정 TripPlan의 DailyPlan만 삭제하고 다른 TripPlan의 DailyPlan은 유지한다")
    void shouldDeleteOnlySpecificTripPlanDailyPlans() {
      // given
      Member member = memberRepository.save(MemberFixture.create());
      TripPlan tripPlan1 = TripPlanFixture.createWithMember(member);
      TripPlan tripPlan2 = TripPlanFixture.createWithMember(member);

      DailyPlan dailyPlan1 =
          DailyPlan.builder()
              .tripPlan(tripPlan1)
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .build();
      DailyPlan dailyPlan2 =
          DailyPlan.builder()
              .tripPlan(tripPlan2)
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .build();

      tripPlan1.addDailyPlan(dailyPlan1);
      tripPlan2.addDailyPlan(dailyPlan2);
      tripPlanRepository.save(tripPlan1);
      tripPlanRepository.save(tripPlan2);

      // when
      dailyPlanRepository.deleteByTripPlanId(tripPlan1.getId());

      // then
      List<DailyPlan> tripPlan1Result = dailyPlanRepository.findAllByTripPlanId(tripPlan1.getId());
      List<DailyPlan> tripPlan2Result = dailyPlanRepository.findAllByTripPlanId(tripPlan2.getId());

      assertThat(tripPlan1Result).isEmpty();
      assertThat(tripPlan2Result).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 tripPlanId로 삭제해도 예외가 발생하지 않는다")
    void shouldNotThrowExceptionForNonExistentTripPlanId() {
      // given
      Long nonExistentTripPlanId = 999L;

      // when & then
      dailyPlanRepository.deleteByTripPlanId(nonExistentTripPlanId);
      // 예외가 발생하지 않음을 확인
    }
  }
}
