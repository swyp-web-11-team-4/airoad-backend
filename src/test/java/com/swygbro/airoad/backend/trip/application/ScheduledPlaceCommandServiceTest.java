package com.swygbro.airoad.backend.trip.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.fixture.content.PlaceFixture;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.fixture.trip.TripPlanFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceUpdateRequest;
import com.swygbro.airoad.backend.trip.domain.entity.DailyPlan;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledPlace;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduledPlaceCommandServiceTest {

  @InjectMocks private ScheduledPlaceCommandService scheduledPlaceCommandService;

  @Mock private TripPlanRepository tripPlanRepository;

  @Mock private PlaceRepository placeRepository;

  private Member member;
  private TripPlan tripPlan;
  private Place place;

  @BeforeEach
  void setUp() {
    member = MemberFixture.withId(1L, MemberFixture.create());
    tripPlan = TripPlanFixture.withId(1L, TripPlanFixture.createWithMember(member));
    DailyPlan dailyPlan =
        DailyPlan.builder().tripPlan(tripPlan).dayNumber(1).date(LocalDate.now()).build();
    tripPlan.getDailyPlans().add(dailyPlan);
    place = PlaceFixture.withId(1L, PlaceFixture.create());
  }

  @Test
  @DisplayName("여행 일정에 새로운 장소를 추가하면, 해당 일차에 장소가 추가되어야 한다.")
  void saveScheduledPlace_Success() {
    // given
    Long tripPlanId = tripPlan.getId();
    Integer dayNumber = 1;
    ScheduledPlaceCreateRequest request =
        new ScheduledPlaceCreateRequest(
            place.getId(),
            1,
            ScheduledCategory.MORNING,
            LocalTime.of(9, 0),
            LocalTime.of(11, 0),
            30,
            Transportation.PUBLIC_TRANSIT);

    given(tripPlanRepository.findByIdWithDetails(tripPlanId)).willReturn(Optional.of(tripPlan));
    given(placeRepository.findById(place.getId())).willReturn(Optional.of(place));

    // when
    scheduledPlaceCommandService.saveScheduledPlace(
        member.getEmail(), tripPlanId, dayNumber, request);

    // then
    DailyPlan dailyPlan = tripPlan.getDailyPlans().get(0);
    assertThat(dailyPlan.getScheduledPlaces()).hasSize(1);
    ScheduledPlace savedPlace = dailyPlan.getScheduledPlaces().get(0);
    assertThat(savedPlace.getPlace().getId()).isEqualTo(place.getId());
    verify(tripPlanRepository).findByIdWithDetails(tripPlanId);
    verify(placeRepository).findById(place.getId());
  }

  @Test
  @DisplayName("존재하지 않는 여행 계획에 장소를 추가하려고 하면, BusinessException이 발생해야 한다.")
  void saveScheduledPlace_TripPlanNotFound_ThrowsException() {
    // given
    Long invalidTripPlanId = 999L;
    ScheduledPlaceCreateRequest request =
        new ScheduledPlaceCreateRequest(
            place.getId(),
            1,
            ScheduledCategory.MORNING,
            LocalTime.of(9, 0),
            LocalTime.of(11, 0),
            30,
            Transportation.PUBLIC_TRANSIT);

    given(tripPlanRepository.findByIdWithDetails(invalidTripPlanId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(
            () ->
                scheduledPlaceCommandService.saveScheduledPlace(
                    member.getEmail(), invalidTripPlanId, 1, request))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(TripErrorCode.TRIP_PLAN_NOT_FOUND);
    verify(placeRepository, never()).findById(any());
  }

  @Test
  @DisplayName("다른 사용자의 여행 계획에 장소를 추가하려고 하면, BusinessException이 발생해야 한다.")
  void saveScheduledPlace_Forbidden_ThrowsException() {
    // given
    Long tripPlanId = tripPlan.getId();
    String otherUserEmail = "other@example.com";
    ScheduledPlaceCreateRequest request =
        new ScheduledPlaceCreateRequest(
            place.getId(),
            1,
            ScheduledCategory.MORNING,
            LocalTime.of(9, 0),
            LocalTime.of(11, 0),
            30,
            Transportation.PUBLIC_TRANSIT);

    given(tripPlanRepository.findByIdWithDetails(tripPlanId)).willReturn(Optional.of(tripPlan));

    // when & then
    assertThatThrownBy(
            () ->
                scheduledPlaceCommandService.saveScheduledPlace(
                    otherUserEmail, tripPlanId, 1, request))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(TripErrorCode.TRIP_PLAN_FORBIDDEN);
    verify(placeRepository, never()).findById(any());
  }

  @Test
  @DisplayName("예정된 장소의 정보를 수정하면, 정보가 성공적으로 변경되어야 한다.")
  void updateScheduledPlace_Success() {
    // given
    DailyPlan dailyPlan = tripPlan.getDailyPlans().get(0);
    ScheduledPlace scheduledPlace =
        ScheduledPlace.builder().dailyPlan(dailyPlan).visitOrder(1).build();
    dailyPlan.addScheduledPlace(scheduledPlace);

    Long tripPlanId = tripPlan.getId();
    Integer dayNumber = 1;
    Integer visitOrder = 1;
    ScheduledPlaceUpdateRequest request =
        new ScheduledPlaceUpdateRequest(
            2,
            ScheduledCategory.AFTERNOON,
            LocalTime.of(14, 0),
            LocalTime.of(16, 0),
            20,
            Transportation.WALKING);

    given(tripPlanRepository.findByIdWithDetails(tripPlanId)).willReturn(Optional.of(tripPlan));

    // when
    scheduledPlaceCommandService.updateScheduledPlace(
        member.getEmail(), tripPlanId, dayNumber, visitOrder, request);

    // then
    assertThat(scheduledPlace.getVisitOrder()).isEqualTo(request.visitOrder());
    assertThat(scheduledPlace.getCategory()).isEqualTo(request.category());
    assertThat(scheduledPlace.getStartTime()).isEqualTo(request.startTime());
    assertThat(scheduledPlace.getEndTime()).isEqualTo(request.endTime());
    assertThat(scheduledPlace.getTravelSegment().getTravelTime()).isEqualTo(request.travelTime());
    assertThat(scheduledPlace.getTravelSegment().getTransportation())
        .isEqualTo(request.transportation());
    verify(tripPlanRepository).findByIdWithDetails(tripPlanId);
  }

  @Test
  @DisplayName("존재하지 않는 일차의 장소를 수정하려고 하면, BusinessException이 발생해야 한다.")
  void updateScheduledPlace_DailyPlanNotFound_ThrowsException() {
    // given
    Long tripPlanId = tripPlan.getId();
    Integer invalidDayNumber = 999;
    ScheduledPlaceUpdateRequest request =
        new ScheduledPlaceUpdateRequest(
            1,
            ScheduledCategory.AFTERNOON,
            LocalTime.of(14, 0),
            LocalTime.of(16, 0),
            20,
            Transportation.WALKING);

    given(tripPlanRepository.findByIdWithDetails(tripPlanId)).willReturn(Optional.of(tripPlan));

    // when & then
    assertThatThrownBy(
            () ->
                scheduledPlaceCommandService.updateScheduledPlace(
                    member.getEmail(), tripPlanId, invalidDayNumber, 1, request))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(TripErrorCode.DAILY_PLAN_NOT_FOUND);
  }

  @Test
  @DisplayName("여행 일정에서 특정 장소를 삭제하면, 해당 장소가 일정에서 제거되어야 한다.")
  void deleteScheduledPlace_Success() {
    // given
    DailyPlan dailyPlan = tripPlan.getDailyPlans().get(0);
    ScheduledPlace scheduledPlace =
        ScheduledPlace.builder().dailyPlan(dailyPlan).visitOrder(1).build();
    dailyPlan.addScheduledPlace(scheduledPlace);

    Long tripPlanId = tripPlan.getId();
    Integer dayNumber = 1;
    Integer visitOrder = 1;

    given(tripPlanRepository.findByIdWithDetails(tripPlanId)).willReturn(Optional.of(tripPlan));

    // when
    scheduledPlaceCommandService.deleteScheduledPlace(
        member.getEmail(), tripPlanId, dayNumber, visitOrder);

    // then
    assertThat(dailyPlan.getScheduledPlaces()).isEmpty();
    verify(tripPlanRepository).findByIdWithDetails(tripPlanId);
  }

  @Test
  @DisplayName("존재하지 않는 방문 순서의 장소를 삭제하려고 하면, BusinessException이 발생해야 한다.")
  void deleteScheduledPlace_ScheduledPlaceNotFound_ThrowsException() {
    // given
    Long tripPlanId = tripPlan.getId();
    Integer dayNumber = 1;
    Integer invalidVisitOrder = 999;

    given(tripPlanRepository.findByIdWithDetails(tripPlanId)).willReturn(Optional.of(tripPlan));

    // when & then
    assertThatThrownBy(
            () ->
                scheduledPlaceCommandService.deleteScheduledPlace(
                    member.getEmail(), tripPlanId, dayNumber, invalidVisitOrder))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(TripErrorCode.SCHEDULED_PLACE_NOT_FOUND);
  }
}
