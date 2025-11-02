package com.swygbro.airoad.backend.trip.presentation.message;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.ai.domain.event.DailyPlanGeneratedEvent;
import com.swygbro.airoad.backend.trip.application.DailyPlanUseCase;
import com.swygbro.airoad.backend.trip.domain.dto.request.DailyPlanCreateRequest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class TripPlanPersistenceListenerTest {

  @Mock private DailyPlanUseCase dailyPlanUseCase;

  @InjectMocks private TripPlanPersistenceListener tripPlanPersistenceListener;

  @Nested
  @DisplayName("AI가 일정을 생성했을 때")
  class HandleDailyPlanGeneratedTests {

    @Test
    @DisplayName("생성된 일정을 DB에 저장한다")
    void 생성된_일정을_DB에_저장() {
      // given
      DailyPlanCreateRequest dailyPlan =
          DailyPlanCreateRequest.builder()
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .places(Collections.emptyList())
              .build();

      String username = "testUser";

      DailyPlanGeneratedEvent event =
          DailyPlanGeneratedEvent.builder()
              .chatRoomId(1L)
              .tripPlanId(100L)
              .username(username)
              .dailyPlan(dailyPlan)
              .build();

      // when
      tripPlanPersistenceListener.handleDailyPlanGenerated(event);

      // then
      verify(dailyPlanUseCase).saveDailyPlan(eq(1L), eq(100L), eq(username), eq(dailyPlan));
    }

    @Test
    @DisplayName("여러 일차의 일정을 순차적으로 저장한다")
    void 여러_일차의_일정을_순차적으로_저장() {
      // given
      DailyPlanCreateRequest day1 =
          DailyPlanCreateRequest.builder()
              .dayNumber(1)
              .date(LocalDate.of(2025, 12, 1))
              .places(Collections.emptyList())
              .build();

      String username = "testUser";

      DailyPlanCreateRequest day2 =
          DailyPlanCreateRequest.builder()
              .dayNumber(2)
              .date(LocalDate.of(2025, 12, 2))
              .places(Collections.emptyList())
              .build();

      DailyPlanGeneratedEvent event1 =
          DailyPlanGeneratedEvent.builder()
              .chatRoomId(1L)
              .tripPlanId(100L)
              .username(username)
              .dailyPlan(day1)
              .build();

      DailyPlanGeneratedEvent event2 =
          DailyPlanGeneratedEvent.builder()
              .chatRoomId(1L)
              .tripPlanId(100L)
              .username(username)
              .dailyPlan(day2)
              .build();

      // when
      tripPlanPersistenceListener.handleDailyPlanGenerated(event1);
      tripPlanPersistenceListener.handleDailyPlanGenerated(event2);

      // then
      verify(dailyPlanUseCase, times(1)).saveDailyPlan(eq(1L), eq(100L), eq(username), eq(day1));
      verify(dailyPlanUseCase, times(1)).saveDailyPlan(eq(1L), eq(100L), eq(username), eq(day2));
    }
  }
}
