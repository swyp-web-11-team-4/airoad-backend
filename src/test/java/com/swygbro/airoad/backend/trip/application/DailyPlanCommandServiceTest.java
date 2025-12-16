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

import com.swygbro.airoad.backend.common.infrastructure.encryption.SHA256Hasher;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.fixture.trip.TripPlanFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.trip.domain.dto.request.DailyPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.domain.event.DailyPlanSavedEvent;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyPlanCommandServiceTest {

  @InjectMocks private DailyPlanCommandService dailyPlanCommandService;

  @Mock private SHA256Hasher sha256Hasher;

  @Mock private TripPlanRepository tripPlanRepository;

  @Mock private ApplicationEventPublisher eventPublisher;

  private Member member;
  private TripPlan tripPlan;

  @BeforeEach
  void setUp() {
    member = MemberFixture.withId(1L, MemberFixture.create());
    tripPlan = TripPlanFixture.withId(1L, TripPlanFixture.createWithMember(member));
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
}
