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
import com.swygbro.airoad.backend.fixture.trip.TripPlanFixture;
import com.swygbro.airoad.backend.trip.domain.dto.request.DailyPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceCreateRequest;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.domain.event.DailyPlanSavedEvent;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class DailyPlanServiceTest {

  @Mock private TripPlanRepository tripPlanRepository;

  @Mock private PlaceRepository placeRepository;

  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private DailyPlanService dailyPlanService;

  @Nested
  @DisplayName("saveDailyPlan 메서드는")
  class SaveDailyPlan {

    @Test
    @DisplayName("유효한 요청으로 일일 계획을 저장하면 DailyPlanSavedEvent를 발행한다")
    void shouldSaveDailyPlanAndPublishEvent() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 1L;
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
      given(placeRepository.findById(1L)).willReturn(Optional.of(place));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(tripPlan);

      // when
      dailyPlanService.saveDailyPlan(chatRoomId, tripPlanId, request);

      // then
      verify(tripPlanRepository).findById(tripPlanId);
      verify(placeRepository).findById(1L);
      verify(tripPlanRepository).save(any(TripPlan.class));

      ArgumentCaptor<DailyPlanSavedEvent> eventCaptor =
          ArgumentCaptor.forClass(DailyPlanSavedEvent.class);
      verify(eventPublisher).publishEvent(eventCaptor.capture());

      DailyPlanSavedEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.chatRoomId()).isEqualTo(chatRoomId);
      assertThat(capturedEvent.tripPlanId()).isEqualTo(tripPlanId);
      assertThat(capturedEvent.dailyPlan()).isNotNull();
      assertThat(capturedEvent.dailyPlan().dayNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 TripPlan ID로 저장하면 TRIP_PLAN_NOT_FOUND 예외를 발생시킨다")
    void shouldThrowExceptionWhenTripPlanNotFound() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 999L;
      DailyPlanCreateRequest request =
          DailyPlanCreateRequest.builder()
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .places(List.of())
              .build();

      given(tripPlanRepository.findById(tripPlanId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> dailyPlanService.saveDailyPlan(chatRoomId, tripPlanId, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", TripErrorCode.TRIP_PLAN_NOT_FOUND);

      verify(tripPlanRepository).findById(tripPlanId);
      verify(placeRepository, never()).findById(anyLong());
      verify(tripPlanRepository, never()).save(any(TripPlan.class));
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("존재하지 않는 Place ID로 저장하면 PLACE_NOT_FOUND 예외를 발생시킨다")
    void shouldThrowExceptionWhenPlaceNotFound() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 1L;
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
      given(placeRepository.findById(999L)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> dailyPlanService.saveDailyPlan(chatRoomId, tripPlanId, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", TripErrorCode.PLACE_NOT_FOUND);

      verify(tripPlanRepository).findById(tripPlanId);
      verify(placeRepository).findById(999L);
      verify(tripPlanRepository, never()).save(any(TripPlan.class));
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("여러 개의 장소가 포함된 일일 계획을 저장한다")
    void shouldSaveDailyPlanWithMultiplePlaces() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 1L;
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
      given(placeRepository.findById(1L)).willReturn(Optional.of(place1));
      given(placeRepository.findById(2L)).willReturn(Optional.of(place2));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(tripPlan);

      // when
      dailyPlanService.saveDailyPlan(chatRoomId, tripPlanId, request);

      // then
      verify(placeRepository).findById(1L);
      verify(placeRepository).findById(2L);
      verify(tripPlanRepository).save(any(TripPlan.class));
      verify(eventPublisher).publishEvent(any(DailyPlanSavedEvent.class));
    }

    @Test
    @DisplayName("장소가 없는 일일 계획을 저장하면 이벤트를 발행한다")
    void shouldSaveDailyPlanWithoutPlaces() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 1L;
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
      dailyPlanService.saveDailyPlan(chatRoomId, tripPlanId, request);

      // then
      verify(tripPlanRepository).findById(tripPlanId);
      verify(placeRepository, never()).findById(anyLong());
      verify(tripPlanRepository).save(any(TripPlan.class));
      verify(eventPublisher).publishEvent(any(DailyPlanSavedEvent.class));
    }

    @Test
    @DisplayName("다양한 카테고리의 장소를 포함한 일일 계획을 저장한다")
    void shouldSaveDailyPlanWithVariousCategories() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 1L;
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
      given(placeRepository.findById(1L)).willReturn(Optional.of(place1));
      given(placeRepository.findById(2L)).willReturn(Optional.of(place2));
      given(placeRepository.findById(3L)).willReturn(Optional.of(place3));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(tripPlan);

      // when
      dailyPlanService.saveDailyPlan(chatRoomId, tripPlanId, request);

      // then
      verify(placeRepository, times(3)).findById(anyLong());
      verify(tripPlanRepository).save(any(TripPlan.class));
      verify(eventPublisher).publishEvent(any(DailyPlanSavedEvent.class));
    }
  }
}
