package com.swygbro.airoad.backend.ai.presentation.message;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.application.common.AiUseCase;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationRequestedEvent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class TripPlanGenerationListenerTest {

  @Mock private AiUseCase aiUseCase;

  @InjectMocks private TripPlanGenerationListener tripPlanGenerationListener;

  @Nested
  @DisplayName("여행 일정 생성 요청 이벤트를 수신할 때")
  class HandleTripPlanGenerationRequestedTests {

    @Test
    @DisplayName("이벤트 정보를 AiDailyPlanRequest로 변환하여 tripAgent에게 전달한다")
    void 이벤트_정보를_AiDailyPlanRequest로_변환하여_tripAgent에게_전달() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 100L;
      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(List.of(PlaceThemeType.HEALING, PlaceThemeType.CULTURE_ART))
              .startDate(LocalDate.of(2025, 12, 1))
              .duration(3)
              .region("제주")
              .peopleCount(2)
              .build();

      TripPlanGenerationRequestedEvent event =
          TripPlanGenerationRequestedEvent.builder()
              .chatRoomId(chatRoomId)
              .tripPlanId(tripPlanId)
              .request(request)
              .build();

      // when
      tripPlanGenerationListener.handleTripPlanGenerationRequested(event);

      // then
      ArgumentCaptor<AiDailyPlanRequest> requestCaptor =
          ArgumentCaptor.forClass(AiDailyPlanRequest.class);
      verify(aiUseCase).agentCall(eq(AgentType.TRIP_AGENT), requestCaptor.capture());

      AiDailyPlanRequest capturedRequest = requestCaptor.getValue();
      assertThat(capturedRequest.chatRoomId()).isEqualTo(chatRoomId);
      assertThat(capturedRequest.tripPlanId()).isEqualTo(tripPlanId);
      assertThat(capturedRequest.themes())
          .containsExactly(PlaceThemeType.HEALING, PlaceThemeType.CULTURE_ART);
      assertThat(capturedRequest.startDate()).isEqualTo(LocalDate.of(2025, 12, 1));
      assertThat(capturedRequest.duration()).isEqualTo(3);
      assertThat(capturedRequest.region()).isEqualTo("제주");
      assertThat(capturedRequest.peopleCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("복수 테마를 올바르게 전달한다")
    void 복수_테마를_올바르게_전달() {
      // given
      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(
                  List.of(
                      PlaceThemeType.HEALING,
                      PlaceThemeType.CULTURE_ART,
                      PlaceThemeType.EXPERIENCE_ACTIVITY,
                      PlaceThemeType.RESTAURANT))
              .startDate(LocalDate.of(2025, 9, 1))
              .duration(4)
              .region("서울")
              .peopleCount(4)
              .build();

      TripPlanGenerationRequestedEvent event =
          TripPlanGenerationRequestedEvent.builder()
              .chatRoomId(2L)
              .tripPlanId(200L)
              .request(request)
              .build();

      // when
      tripPlanGenerationListener.handleTripPlanGenerationRequested(event);

      // then
      ArgumentCaptor<AiDailyPlanRequest> requestCaptor =
          ArgumentCaptor.forClass(AiDailyPlanRequest.class);
      verify(aiUseCase).agentCall(eq(AgentType.TRIP_AGENT), requestCaptor.capture());

      AiDailyPlanRequest capturedRequest = requestCaptor.getValue();
      assertThat(capturedRequest.themes()).hasSize(4);
      assertThat(capturedRequest.themes())
          .containsExactly(
              PlaceThemeType.HEALING,
              PlaceThemeType.CULTURE_ART,
              PlaceThemeType.EXPERIENCE_ACTIVITY,
              PlaceThemeType.RESTAURANT);
    }

    @Test
    @DisplayName("정확히 tripAgent를 호출한다")
    void 정확히_tripAgent를_호출() {
      // given
      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(List.of(PlaceThemeType.HEALING))
              .startDate(LocalDate.of(2025, 8, 1))
              .duration(2)
              .region("부산")
              .peopleCount(3)
              .build();

      TripPlanGenerationRequestedEvent event =
          TripPlanGenerationRequestedEvent.builder()
              .chatRoomId(3L)
              .tripPlanId(300L)
              .request(request)
              .build();

      // when
      tripPlanGenerationListener.handleTripPlanGenerationRequested(event);

      // then
      verify(aiUseCase, times(1))
          .agentCall(eq(AgentType.TRIP_AGENT), any(AiDailyPlanRequest.class));
    }
  }
}
