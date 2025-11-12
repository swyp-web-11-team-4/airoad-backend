package com.swygbro.airoad.backend.trip.presentation.message;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.trip.domain.dto.TripPlanProgressMessage;
import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;
import com.swygbro.airoad.backend.trip.domain.event.DailyPlanSavedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationCancelledEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationCompletedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationErrorEvent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class TripPlanNotificationListenerTest {

  @Mock private SimpMessagingTemplate messagingTemplate;

  @InjectMocks private TripPlanNotificationListener tripPlanNotificationListener;

  @Nested
  @DisplayName("일정 저장 완료 이벤트를 수신할 때")
  class HandleDailyPlanSavedTests {

    @Test
    @DisplayName("WebSocket을 통해 일정 생성 채널로 일정 데이터를 전송한다")
    void WebSocket으로_일정_생성_진행_상황_전송() {
      // given
      DailyPlanResponse dailyPlan =
          DailyPlanResponse.builder()
              .dayNumber(1)
              .date("2025-12-01")
              .scheduledPlaces(Collections.emptyList())
              .build();

      String username = "testUser";

      DailyPlanSavedEvent event =
          DailyPlanSavedEvent.builder()
              .chatRoomId(1L)
              .tripPlanId(100L)
              .username(username)
              .dailyPlan(dailyPlan)
              .build();

      // when
      tripPlanNotificationListener.handleDailyPlanSaved(event);

      // then
      ArgumentCaptor<TripPlanProgressMessage> messageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate)
          .convertAndSendToUser(eq("testUser"), eq("/sub/schedule/100"), messageCaptor.capture());

      TripPlanProgressMessage message = messageCaptor.getValue();
      assertThat(message.type())
          .isEqualTo(TripPlanProgressMessage.MessageType.DAILY_PLAN_GENERATED);
      assertThat(message.tripPlanId()).isEqualTo(100L);
      assertThat(message.dailyPlan()).isEqualTo(dailyPlan);
    }
  }

  @Nested
  @DisplayName("전체 일정 생성 완료 이벤트를 수신할 때")
  class HandleTripPlanGenerationCompletedTests {

    @Test
    @DisplayName("WebSocket을 통해 채팅 채널로 완료 메시지를 전송한다")
    void WebSocket으로_완료_메시지_전송() {
      // given
      String username = "testUser";

      TripPlanGenerationCompletedEvent event =
          TripPlanGenerationCompletedEvent.builder()
              .chatRoomId(1L)
              .tripPlanId(100L)
              .username(username)
              .message("여행 일정 생성이 완료되었습니다")
              .build();

      // when
      tripPlanNotificationListener.handleTripPlanGenerationCompleted(event);

      // then
      ArgumentCaptor<TripPlanProgressMessage> messageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate)
          .convertAndSendToUser(eq("testUser"), eq("/sub/schedule/100"), messageCaptor.capture());

      TripPlanProgressMessage message = messageCaptor.getValue();
      assertThat(message.type()).isEqualTo(TripPlanProgressMessage.MessageType.COMPLETED);
      assertThat(message.tripPlanId()).isEqualTo(100L);
      assertThat(message.message()).isEqualTo("여행 일정 생성이 완료되었습니다");
    }
  }

  @Nested
  @DisplayName("일정 생성 오류 이벤트를 수신할 때")
  class HandleTripPlanGenerationErrorTests {

    @Test
    @DisplayName("WebSocket을 통해 에러 채널로 오류 메시지를 전송한다")
    void WebSocket으로_에러_메시지_전송() {
      // given
      String username = "testUser";

      TripPlanGenerationErrorEvent event =
          TripPlanGenerationErrorEvent.builder()
              .chatRoomId(1L)
              .tripPlanId(100L)
              .username(username)
              .errorCode(AiErrorCode.TRIP_PLAN_GENERATION_ERROR)
              .build();

      // when
      tripPlanNotificationListener.handleTripPlanGenerationError(event);

      // then
      ArgumentCaptor<TripPlanProgressMessage> messageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate)
          .convertAndSendToUser(eq("testUser"), eq("/sub/errors/1"), messageCaptor.capture());

      TripPlanProgressMessage message = messageCaptor.getValue();
      assertThat(message.type()).isEqualTo(TripPlanProgressMessage.MessageType.ERROR);
      assertThat(message.tripPlanId()).isEqualTo(100L);
      assertThat(message.errorCode()).isEqualTo(AiErrorCode.TRIP_PLAN_GENERATION_ERROR.getCode());
    }
  }

  @Nested
  @DisplayName("일정 생성 취소 이벤트를 수신할 때")
  class HandleTripPlanGenerationCancelledTests {

    @Test
    @DisplayName("WebSocket을 통해 채팅 채널로 취소 메시지를 전송한다")
    void WebSocket으로_취소_메시지_전송() {
      // given
      String username = "testUser";

      TripPlanGenerationCancelledEvent event =
          TripPlanGenerationCancelledEvent.builder()
              .chatRoomId(1L)
              .tripPlanId(100L)
              .username(username)
              .reason("사용자 요청")
              .build();

      // when
      tripPlanNotificationListener.handleTripPlanGenerationCancelled(event);

      // then
      ArgumentCaptor<TripPlanProgressMessage> messageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate)
          .convertAndSendToUser(eq("testUser"), eq("/sub/schedule/100"), messageCaptor.capture());

      TripPlanProgressMessage message = messageCaptor.getValue();
      assertThat(message.type()).isEqualTo(TripPlanProgressMessage.MessageType.CANCELLED);
      assertThat(message.tripPlanId()).isEqualTo(100L);
      assertThat(message.message()).contains("사용자 요청");
    }
  }
}
