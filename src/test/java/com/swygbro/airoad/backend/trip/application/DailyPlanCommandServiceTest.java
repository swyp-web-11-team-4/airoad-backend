package com.swygbro.airoad.backend.trip.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.fixture.content.PlaceFixture;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.fixture.trip.DailyPlanFixture;
import com.swygbro.airoad.backend.fixture.trip.ScheduledPlaceFixture;
import com.swygbro.airoad.backend.fixture.trip.TripPlanFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.trip.domain.dto.request.DailyPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.entity.DailyPlan;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledPlace;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.domain.event.DailyPlanSavedEvent;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyPlanCommandServiceTest {

  @InjectMocks private DailyPlanCommandService dailyPlanCommandService;

  @Mock private TripPlanRepository tripPlanRepository;

  @Mock private PlaceRepository placeRepository;

  @Mock private ApplicationEventPublisher eventPublisher;

  private Member member;
  private TripPlan tripPlan;
  private Place place1;
  private Place place2;

  @BeforeEach
  void setUp() {
    member = MemberFixture.withId(1L, MemberFixture.create());
    tripPlan = TripPlanFixture.withId(1L, TripPlanFixture.createWithMember(member));
    place1 = PlaceFixture.withId(1L, PlaceFixture.create());
    place2 = PlaceFixture.withId(2L, PlaceFixture.create());
  }

  @Test
  @DisplayName("일일 계획 저장이 요청되면 새로운 일일 계획이 생성되어야 한다")
  void 일일_계획_저장이_요청되면_새로운_일일_계획이_생성되어야_한다() {
    // given
    DailyPlanCreateRequest request =
        new DailyPlanCreateRequest(1, LocalDate.now(), "title", "description", List.of());
    given(tripPlanRepository.findById(tripPlan.getId())).willReturn(Optional.of(tripPlan));
    given(tripPlanRepository.save(any(TripPlan.class))).willReturn(tripPlan);

    // when
    dailyPlanCommandService.saveDailyPlan(1L, tripPlan.getId(), member.getEmail(), request);

    // then
    assertThat(tripPlan.getDailyPlans()).hasSize(1);
    verify(eventPublisher).publishEvent(any(DailyPlanSavedEvent.class));
  }

  @Test
  @DisplayName("다른 날짜의 장소 교환이 요청되면 두 장소의 정보가 교환되어야 한다")
  void 다른_날짜의_장소_교환이_요청되면_두_장소의_정보가_교환되어야_한다() {
    // given
    DailyPlan dailyPlan1 =
        DailyPlanFixture.builder().tripPlan(tripPlan).date(LocalDate.now()).dayNumber(1).build();
    DailyPlan dailyPlan2 =
        DailyPlanFixture.builder()
            .tripPlan(tripPlan)
            .date(LocalDate.now().plusDays(1))
            .dayNumber(2)
            .build();

    ScheduledPlace scheduledPlace1 =
        ScheduledPlaceFixture.builder()
            .dailyPlan(dailyPlan1)
            .place(place1)
            .visitOrder(1)
            .category(ScheduledCategory.MORNING)
            .build();
    ScheduledPlace scheduledPlace2 =
        ScheduledPlaceFixture.builder()
            .dailyPlan(dailyPlan2)
            .place(place2)
            .visitOrder(1)
            .category(ScheduledCategory.AFTERNOON)
            .build();

    dailyPlan1.addScheduledPlace(scheduledPlace1);
    dailyPlan2.addScheduledPlace(scheduledPlace2);

    tripPlan.addDailyPlan(dailyPlan1);
    tripPlan.addDailyPlan(dailyPlan2);

    given(tripPlanRepository.findByIdWithDetails(tripPlan.getId()))
        .willReturn(Optional.of(tripPlan));

    // when
    dailyPlanCommandService.swapScheduledPlacesBetweenDays(
        1L, tripPlan.getId(), member.getEmail(), 1, 1, 2, 1);

    // then
    assertThat(scheduledPlace1.getPlace()).isEqualTo(place2);
    assertThat(scheduledPlace2.getPlace()).isEqualTo(place1);
    verify(eventPublisher, atLeast(2)).publishEvent(any(DailyPlanSavedEvent.class));
  }
}
