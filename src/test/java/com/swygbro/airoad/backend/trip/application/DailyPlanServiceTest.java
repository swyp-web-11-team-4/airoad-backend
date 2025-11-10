package com.swygbro.airoad.backend.trip.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.fixture.content.PlaceFixture;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.fixture.trip.DailyPlanFixture;
import com.swygbro.airoad.backend.fixture.trip.TripPlanFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.trip.domain.dto.request.DailyPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;
import com.swygbro.airoad.backend.trip.domain.entity.DailyPlan;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.domain.event.DailyPlanSavedEvent;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;
import com.swygbro.airoad.backend.trip.infrastructure.DailyPlanRepository;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class DailyPlanServiceTest {

  @Mock private TripPlanRepository tripPlanRepository;

  @Mock private PlaceRepository placeRepository;

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private DailyPlanRepository dailyPlanRepository;

  @InjectMocks private DailyPlanCommandService dailyPlanService;

  @Nested
  @DisplayName("saveDailyPlan 메서드는")
  class SaveDailyPlan {

    @Test
    @DisplayName("유효한 요청으로 일일 계획을 저장하면 DailyPlanSavedEvent를 발행한다")
    void shouldSaveDailyPlanAndPublishEvent() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 1L;
      String username = "testUser";
      TripPlan tripPlan = TripPlanFixture.create();
      Place place = PlaceFixture.withId(1L, PlaceFixture.create());

      ScheduledPlaceCreateRequest scheduledPlaceRequest =
          ScheduledPlaceCreateRequest.builder()
              .placeId(1L)
              .visitOrder(1)
              .category(ScheduledCategory.MORNING)
              .startTime(LocalTime.of(9, 0))
              .endTime(LocalTime.of(11, 0))
              .travelTime(30)
              .transportation(Transportation.PUBLIC_TRANSIT)
              .build();

      DailyPlanCreateRequest request =
          DailyPlanCreateRequest.builder()
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .places(List.of(scheduledPlaceRequest))
              .build();

      given(tripPlanRepository.findById(tripPlanId)).willReturn(Optional.of(tripPlan));
      given(placeRepository.findAllByIdsWithThemes(anyList())).willReturn(List.of(place));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(tripPlan);

      // when
      dailyPlanService.saveDailyPlan(chatRoomId, tripPlanId, username, request);

      // then
      verify(tripPlanRepository).findById(tripPlanId);
      verify(placeRepository).findAllByIdsWithThemes(List.of(1L));
      verify(tripPlanRepository).save(any(TripPlan.class));

      ArgumentCaptor<DailyPlanSavedEvent> eventCaptor =
          ArgumentCaptor.forClass(DailyPlanSavedEvent.class);
      verify(eventPublisher).publishEvent(eventCaptor.capture());

      DailyPlanSavedEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.chatRoomId()).isEqualTo(chatRoomId);
      assertThat(capturedEvent.tripPlanId()).isEqualTo(tripPlanId);
      assertThat(capturedEvent.username()).isEqualTo(username);
      assertThat(capturedEvent.dailyPlan()).isNotNull();
      assertThat(capturedEvent.dailyPlan().dayNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 TripPlan ID로 저장하면 TRIP_PLAN_NOT_FOUND 예외를 발생시킨다")
    void shouldThrowExceptionWhenTripPlanNotFound() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 999L;
      String username = "testUser";
      DailyPlanCreateRequest request =
          DailyPlanCreateRequest.builder()
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .places(List.of())
              .build();

      given(tripPlanRepository.findById(tripPlanId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(
              () -> dailyPlanService.saveDailyPlan(chatRoomId, tripPlanId, username, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", TripErrorCode.TRIP_PLAN_NOT_FOUND);

      verify(tripPlanRepository).findById(tripPlanId);
      verify(placeRepository, never()).findAllByIdsWithThemes(anyList());
      verify(tripPlanRepository, never()).save(any(TripPlan.class));
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("존재하지 않는 Place ID로 저장하면 PLACE_NOT_FOUND 예외를 발생시킨다")
    void shouldThrowExceptionWhenPlaceNotFound() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 1L;
      String username = "testUser";
      TripPlan tripPlan = TripPlanFixture.create();

      ScheduledPlaceCreateRequest scheduledPlaceRequest =
          ScheduledPlaceCreateRequest.builder()
              .placeId(999L)
              .visitOrder(1)
              .category(ScheduledCategory.MORNING)
              .startTime(LocalTime.of(9, 0))
              .endTime(LocalTime.of(11, 0))
              .travelTime(30)
              .transportation(Transportation.PUBLIC_TRANSIT)
              .build();

      DailyPlanCreateRequest request =
          DailyPlanCreateRequest.builder()
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .places(List.of(scheduledPlaceRequest))
              .build();

      given(tripPlanRepository.findById(tripPlanId)).willReturn(Optional.of(tripPlan));
      given(placeRepository.findAllByIdsWithThemes(anyList())).willReturn(List.of());

      // when & then
      assertThatThrownBy(
              () -> dailyPlanService.saveDailyPlan(chatRoomId, tripPlanId, username, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", TripErrorCode.PLACE_NOT_FOUND);

      verify(tripPlanRepository).findById(tripPlanId);
      verify(placeRepository).findAllByIdsWithThemes(List.of(999L));
      verify(tripPlanRepository, never()).save(any(TripPlan.class));
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("여러 개의 장소가 포함된 일일 계획을 저장한다")
    void shouldSaveDailyPlanWithMultiplePlaces() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 1L;
      String username = "testUser";
      TripPlan tripPlan = TripPlanFixture.create();
      Place place1 = PlaceFixture.withId(1L, PlaceFixture.create());
      Place place2 = PlaceFixture.withId(2L, PlaceFixture.createGangnam());

      ScheduledPlaceCreateRequest scheduledPlace1 =
          ScheduledPlaceCreateRequest.builder()
              .placeId(1L)
              .visitOrder(1)
              .category(ScheduledCategory.MORNING)
              .startTime(LocalTime.of(9, 0))
              .endTime(LocalTime.of(11, 0))
              .travelTime(0)
              .transportation(Transportation.WALKING)
              .build();

      ScheduledPlaceCreateRequest scheduledPlace2 =
          ScheduledPlaceCreateRequest.builder()
              .placeId(2L)
              .visitOrder(2)
              .category(ScheduledCategory.LUNCH)
              .startTime(LocalTime.of(12, 0))
              .endTime(LocalTime.of(13, 0))
              .travelTime(30)
              .transportation(Transportation.PUBLIC_TRANSIT)
              .build();

      DailyPlanCreateRequest request =
          DailyPlanCreateRequest.builder()
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .places(List.of(scheduledPlace1, scheduledPlace2))
              .build();

      given(tripPlanRepository.findById(tripPlanId)).willReturn(Optional.of(tripPlan));
      given(placeRepository.findAllByIdsWithThemes(anyList())).willReturn(List.of(place1, place2));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(tripPlan);

      // when
      dailyPlanService.saveDailyPlan(chatRoomId, tripPlanId, username, request);

      // then
      verify(placeRepository).findAllByIdsWithThemes(List.of(1L, 2L));
      verify(tripPlanRepository).save(any(TripPlan.class));
      verify(eventPublisher).publishEvent(any(DailyPlanSavedEvent.class));
    }

    @Test
    @DisplayName("장소가 없는 일일 계획을 저장하면 이벤트를 발행한다")
    void shouldSaveDailyPlanWithoutPlaces() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 1L;
      String username = "testUser";
      TripPlan tripPlan = TripPlanFixture.create();

      DailyPlanCreateRequest request =
          DailyPlanCreateRequest.builder()
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .places(List.of())
              .build();

      given(tripPlanRepository.findById(tripPlanId)).willReturn(Optional.of(tripPlan));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(tripPlan);

      // when
      dailyPlanService.saveDailyPlan(chatRoomId, tripPlanId, username, request);

      // then
      verify(tripPlanRepository).findById(tripPlanId);
      verify(placeRepository, never()).findAllByIdsWithThemes(anyList());
      verify(tripPlanRepository).save(any(TripPlan.class));
      verify(eventPublisher).publishEvent(any(DailyPlanSavedEvent.class));
    }

    @Test
    @DisplayName("다양한 카테고리의 장소를 포함한 일일 계획을 저장한다")
    void shouldSaveDailyPlanWithVariousCategories() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 1L;
      String username = "testUser";
      TripPlan tripPlan = TripPlanFixture.create();
      Place place1 = PlaceFixture.withId(1L, PlaceFixture.create());
      Place place2 = PlaceFixture.withId(2L, PlaceFixture.createRestaurant());
      Place place3 = PlaceFixture.withId(3L, PlaceFixture.createGangnam());

      ScheduledPlaceCreateRequest morning =
          ScheduledPlaceCreateRequest.builder()
              .placeId(1L)
              .visitOrder(1)
              .category(ScheduledCategory.MORNING)
              .startTime(LocalTime.of(9, 0))
              .endTime(LocalTime.of(11, 0))
              .travelTime(0)
              .transportation(Transportation.WALKING)
              .build();

      ScheduledPlaceCreateRequest lunch =
          ScheduledPlaceCreateRequest.builder()
              .placeId(2L)
              .visitOrder(2)
              .category(ScheduledCategory.LUNCH)
              .startTime(LocalTime.of(12, 0))
              .endTime(LocalTime.of(13, 0))
              .travelTime(30)
              .transportation(Transportation.PUBLIC_TRANSIT)
              .build();

      ScheduledPlaceCreateRequest afternoon =
          ScheduledPlaceCreateRequest.builder()
              .placeId(3L)
              .visitOrder(3)
              .category(ScheduledCategory.AFTERNOON)
              .startTime(LocalTime.of(14, 0))
              .endTime(LocalTime.of(17, 0))
              .travelTime(20)
              .transportation(Transportation.PUBLIC_TRANSIT)
              .build();

      DailyPlanCreateRequest request =
          DailyPlanCreateRequest.builder()
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .places(List.of(morning, lunch, afternoon))
              .build();

      given(tripPlanRepository.findById(tripPlanId)).willReturn(Optional.of(tripPlan));
      given(placeRepository.findAllByIdsWithThemes(anyList()))
          .willReturn(List.of(place1, place2, place3));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(tripPlan);

      // when
      dailyPlanService.saveDailyPlan(chatRoomId, tripPlanId, username, request);

      // then
      verify(placeRepository).findAllByIdsWithThemes(List.of(1L, 2L, 3L));
      verify(tripPlanRepository).save(any(TripPlan.class));
      verify(eventPublisher).publishEvent(any(DailyPlanSavedEvent.class));
    }
  }

  @Nested
  @DisplayName("getDailyPlanListByTripPlanId 메서드는")
  class GetDailyPlanListByTripPlanId {

    @Test
    @DisplayName("유효한 tripPlanId와 memberId로 일차별 일정 목록을 조회한다")
    void shouldReturnDailyPlanList() {
      // given
      Long tripPlanId = 1L;
      Long memberId = 1L;
      Member member = MemberFixture.withId(memberId, MemberFixture.create());
      TripPlan tripPlan = TripPlanFixture.createWithMember(member);

      DailyPlan dailyPlan1 =
          DailyPlanFixture.builder().dayNumber(1).date(LocalDate.of(2025, 12, 1)).build();
      DailyPlan dailyPlan2 =
          DailyPlanFixture.builder().dayNumber(2).date(LocalDate.of(2025, 12, 2)).build();

      List<DailyPlan> dailyPlans = List.of(dailyPlan1, dailyPlan2);

      given(tripPlanRepository.findByIdWithMember(tripPlanId)).willReturn(Optional.of(tripPlan));
      given(dailyPlanRepository.findAllByTripPlanId(tripPlanId)).willReturn(dailyPlans);

      // when
      List<DailyPlanResponse> result =
          dailyPlanService.getDailyPlanListByTripPlanId(tripPlanId, memberId);

      // then
      assertThat(result).hasSize(2);
      assertThat(result.get(0).dayNumber()).isEqualTo(1);
      assertThat(result.get(1).dayNumber()).isEqualTo(2);
      verify(tripPlanRepository).findByIdWithMember(tripPlanId);
      verify(dailyPlanRepository).findAllByTripPlanId(tripPlanId);
    }

    @Test
    @DisplayName("존재하지 않는 TripPlan ID로 조회하면 TRIP_PLAN_NOT_FOUND 예외를 발생시킨다")
    void shouldThrowExceptionWhenTripPlanNotFound() {
      // given
      Long tripPlanId = 999L;
      Long memberId = 1L;

      given(tripPlanRepository.findByIdWithMember(tripPlanId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> dailyPlanService.getDailyPlanListByTripPlanId(tripPlanId, memberId))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", TripErrorCode.TRIP_PLAN_NOT_FOUND);

      verify(tripPlanRepository).findByIdWithMember(tripPlanId);
      verify(dailyPlanRepository, never()).findAllByTripPlanId(anyLong());
    }

    @Test
    @DisplayName("다른 사용자의 TripPlan을 조회하면 TRIP_PLAN_FORBIDDEN 예외를 발생시킨다")
    void shouldThrowExceptionWhenForbidden() {
      // given
      Long tripPlanId = 1L;
      Long requestMemberId = 2L;
      Long ownerMemberId = 1L;

      Member owner = MemberFixture.withId(ownerMemberId, MemberFixture.createWithEmail("owner"));
      TripPlan tripPlan = TripPlanFixture.createWithMember(owner);

      given(tripPlanRepository.findByIdWithMember(tripPlanId)).willReturn(Optional.of(tripPlan));

      // when & then
      assertThatThrownBy(
              () -> dailyPlanService.getDailyPlanListByTripPlanId(tripPlanId, requestMemberId))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", TripErrorCode.TRIP_PLAN_FORBIDDEN);

      verify(tripPlanRepository).findByIdWithMember(tripPlanId);
      verify(dailyPlanRepository, never()).findAllByTripPlanId(anyLong());
    }

    @Test
    @DisplayName("DailyPlan이 없는 TripPlan을 조회하면 빈 리스트를 반환한다")
    void shouldReturnEmptyListWhenNoDailyPlans() {
      // given
      Long tripPlanId = 1L;
      Long memberId = 1L;
      Member member = MemberFixture.withId(memberId, MemberFixture.create());
      TripPlan tripPlan = TripPlanFixture.createWithMember(member);

      given(tripPlanRepository.findByIdWithMember(tripPlanId)).willReturn(Optional.of(tripPlan));
      given(dailyPlanRepository.findAllByTripPlanId(tripPlanId)).willReturn(List.of());

      // when
      List<DailyPlanResponse> result =
          dailyPlanService.getDailyPlanListByTripPlanId(tripPlanId, memberId);

      // then
      assertThat(result).isEmpty();
      verify(tripPlanRepository).findByIdWithMember(tripPlanId);
      verify(dailyPlanRepository).findAllByTripPlanId(tripPlanId);
    }

    @Test
    @DisplayName("여러 일차의 DailyPlan을 일차 순서대로 조회한다")
    void shouldReturnDailyPlansInOrder() {
      // given
      Long tripPlanId = 1L;
      Long memberId = 1L;
      Member member = MemberFixture.withId(memberId, MemberFixture.create());
      TripPlan tripPlan = TripPlanFixture.createWithMember(member);

      DailyPlan dailyPlan1 =
          DailyPlanFixture.builder().dayNumber(1).date(LocalDate.of(2025, 12, 1)).build();
      DailyPlan dailyPlan2 =
          DailyPlanFixture.builder().dayNumber(2).date(LocalDate.of(2025, 12, 2)).build();
      DailyPlan dailyPlan3 =
          DailyPlanFixture.builder().dayNumber(3).date(LocalDate.of(2025, 12, 3)).build();

      List<DailyPlan> dailyPlans = List.of(dailyPlan1, dailyPlan2, dailyPlan3);

      given(tripPlanRepository.findByIdWithMember(tripPlanId)).willReturn(Optional.of(tripPlan));
      given(dailyPlanRepository.findAllByTripPlanId(tripPlanId)).willReturn(dailyPlans);

      // when
      List<DailyPlanResponse> result =
          dailyPlanService.getDailyPlanListByTripPlanId(tripPlanId, memberId);

      // then
      assertThat(result).hasSize(3);
      assertThat(result.get(0).dayNumber()).isEqualTo(1);
      assertThat(result.get(1).dayNumber()).isEqualTo(2);
      assertThat(result.get(2).dayNumber()).isEqualTo(3);
      assertThat(result.get(0).date()).isEqualTo("2025-12-01");
      assertThat(result.get(1).date()).isEqualTo("2025-12-02");
      assertThat(result.get(2).date()).isEqualTo("2025-12-03");
      verify(tripPlanRepository).findByIdWithMember(tripPlanId);
      verify(dailyPlanRepository).findAllByTripPlanId(tripPlanId);
    }
  }
}
