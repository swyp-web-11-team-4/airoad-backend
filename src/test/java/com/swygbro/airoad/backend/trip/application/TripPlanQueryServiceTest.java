package com.swygbro.airoad.backend.trip.application;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.fixture.trip.DailyPlanFixture;
import com.swygbro.airoad.backend.fixture.trip.ScheduledPlaceFixture;
import com.swygbro.airoad.backend.fixture.trip.TripPlanFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanDetailsResponse;
import com.swygbro.airoad.backend.trip.domain.entity.DailyPlan;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledPlace;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TripPlanQueryServiceTest {

  @Mock private TripPlanRepository tripPlanRepository;

  @InjectMocks private TripPlanQueryService tripPlanQueryService;

  private Member member;
  private TripPlan tripPlan;

  @BeforeEach
  void setUp() {
    member = MemberFixture.withId(1L, MemberFixture.createWithEmail("user@example.com"));
    tripPlan = TripPlanFixture.withId(1L, TripPlanFixture.createWithMember(member));
  }

  @Test
  @DisplayName("사용자가 자신의 여행 계획을 조회하면, 상세 정보가 반환되어야 한다")
  void whenUserRequestsOwnTripPlan_thenDetailsShouldBeReturned() {
    // given
    DailyPlan day1 =
        DailyPlanFixture.builder()
            .tripPlan(tripPlan)
            .dayNumber(1)
            .date(LocalDate.of(2025, 12, 1))
            .build();
    DailyPlan day2 =
        DailyPlanFixture.builder()
            .tripPlan(tripPlan)
            .dayNumber(2)
            .date(LocalDate.of(2025, 12, 2))
            .build();

    ScheduledPlace place1_2 = ScheduledPlaceFixture.builder().dailyPlan(day1).visitOrder(2).build();
    ScheduledPlace place1_1 = ScheduledPlaceFixture.builder().dailyPlan(day1).visitOrder(1).build();
    day1.getScheduledPlaces().addAll(Arrays.asList(place1_2, place1_1));

    ScheduledPlace place2_1 = ScheduledPlaceFixture.builder().dailyPlan(day2).visitOrder(1).build();
    day2.getScheduledPlaces().add(place2_1);

    tripPlan.getDailyPlans().addAll(Arrays.asList(day2, day1)); // 순서 섞어서 제공

    given(tripPlanRepository.findByIdWithDetails(tripPlan.getId()))
        .willReturn(Optional.of(tripPlan));

    // when
    TripPlanDetailsResponse response =
        tripPlanQueryService.findTripPlanDetailsById(tripPlan.getId(), member.getEmail());

    // then
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(tripPlan.getId());
    assertThat(response.getTitle()).isEqualTo(tripPlan.getTitle());
    assertThat(response.getDailyPlans()).hasSize(2);
    assertThat(response.getDailyPlans().get(0).dayNumber()).isEqualTo(1);
    assertThat(response.getDailyPlans().get(1).dayNumber()).isEqualTo(2);
    assertThat(response.getDailyPlans().get(0).scheduledPlaces()).hasSize(2);
    assertThat(response.getDailyPlans().get(0).scheduledPlaces().get(0).visitOrder()).isEqualTo(1);
    assertThat(response.getDailyPlans().get(0).scheduledPlaces().get(1).visitOrder()).isEqualTo(2);
  }

  @Test
  @DisplayName("존재하지 않는 여행 계획을 조회하면, 예외가 발생해야 한다")
  void whenRequestingNonExistentTripPlan_thenExceptionShouldBeThrown() {
    // given
    Long nonExistentTripPlanId = 999L;
    given(tripPlanRepository.findByIdWithDetails(nonExistentTripPlanId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(
            () ->
                tripPlanQueryService.findTripPlanDetailsById(
                    nonExistentTripPlanId, member.getEmail()))
        .isInstanceOf(BusinessException.class)
        .hasMessage(TripErrorCode.TRIP_PLAN_NOT_FOUND.getDefaultMessage());
  }

  @Test
  @DisplayName("다른 사용자의 여행 계획을 조회하면, 접근 금지 예외가 발생해야 한다")
  void whenRequestingOthersTripPlan_thenForbiddenExceptionShouldBeThrown() {
    // given
    String otherUsername = "other@example.com";
    given(tripPlanRepository.findByIdWithDetails(tripPlan.getId()))
        .willReturn(Optional.of(tripPlan));

    // when & then
    assertThatThrownBy(
            () -> tripPlanQueryService.findTripPlanDetailsById(tripPlan.getId(), otherUsername))
        .isInstanceOf(BusinessException.class)
        .hasMessage(TripErrorCode.TRIP_PLAN_FORBIDDEN.getDefaultMessage());
  }

  @Test
  @DisplayName("여행 계획에 일정이 없는 경우에도 조회가 성공해야 한다")
  void whenTripPlanHasNoDailyPlans_thenItShouldBeReturnedSuccessfully() {
    // given
    tripPlan.getDailyPlans().clear();
    given(tripPlanRepository.findByIdWithDetails(tripPlan.getId()))
        .willReturn(Optional.of(tripPlan));

    // when
    TripPlanDetailsResponse response =
        tripPlanQueryService.findTripPlanDetailsById(tripPlan.getId(), member.getEmail());

    // then
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(tripPlan.getId());
    assertThat(response.getDailyPlans()).isEmpty();
  }

  @Test
  @DisplayName("일정에 방문 장소가 없는 경우에도 조회가 성공해야 한다")
  void whenDailyPlanHasNoScheduledPlaces_thenItShouldBeReturnedSuccessfully() {
    // given
    DailyPlan day1 =
        DailyPlanFixture.builder()
            .tripPlan(tripPlan)
            .dayNumber(1)
            .date(LocalDate.of(2025, 12, 1))
            .build();
    day1.getScheduledPlaces().clear();
    tripPlan.getDailyPlans().add(day1);

    given(tripPlanRepository.findByIdWithDetails(tripPlan.getId()))
        .willReturn(Optional.of(tripPlan));

    // when
    TripPlanDetailsResponse response =
        tripPlanQueryService.findTripPlanDetailsById(tripPlan.getId(), member.getEmail());

    // then
    assertThat(response).isNotNull();
    assertThat(response.getDailyPlans()).hasSize(1);
    assertThat(response.getDailyPlans().get(0).scheduledPlaces()).isEmpty();
  }
}
