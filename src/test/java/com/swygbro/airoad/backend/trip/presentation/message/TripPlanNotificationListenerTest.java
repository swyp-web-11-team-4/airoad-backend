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
import com.swygbro.airoad.backend.chat.domain.dto.response.ChatStreamDto;
import com.swygbro.airoad.backend.chat.domain.dto.response.MessageStreamType;
import com.swygbro.airoad.backend.common.domain.dto.ErrorResponse;
import com.swygbro.airoad.backend.trip.domain.dto.TripPlanProgressMessage;
import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;
import com.swygbro.airoad.backend.trip.domain.event.DailyPlanSavedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationCancelledEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationCompletedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationErrorEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanUpdateStartedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanUpdatedEvent;

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
    @DisplayName("WebSocket을 통해 일정 생성 채널과 채팅 채널로 데이터를 전송한다")
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

      // then - 일정 채널로 TripPlanProgressMessage 전송 검증
      ArgumentCaptor<TripPlanProgressMessage> tripMessageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate)
          .convertAndSendToUser(
              eq("testUser"), eq("/sub/schedule/100"), tripMessageCaptor.capture());

      TripPlanProgressMessage tripMessage = tripMessageCaptor.getValue();
      assertThat(tripMessage.type())
          .isEqualTo(TripPlanProgressMessage.MessageType.DAILY_PLAN_GENERATED);
      assertThat(tripMessage.tripPlanId()).isEqualTo(100L);
      assertThat(tripMessage.dailyPlan()).isEqualTo(dailyPlan);

      // then - 채팅 채널로 ChatStreamDto 전송 검증
      ArgumentCaptor<ChatStreamDto> chatMessageCaptor =
          ArgumentCaptor.forClass(ChatStreamDto.class);
      verify(messagingTemplate)
          .convertAndSendToUser(eq("testUser"), eq("/sub/chat/1"), chatMessageCaptor.capture());

      ChatStreamDto chatMessage = chatMessageCaptor.getValue();
      assertThat(chatMessage.isComplete()).isTrue();
      assertThat(chatMessage.messageStreamType()).isEqualTo(MessageStreamType.CHAT);
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
      ArgumentCaptor<ChatStreamDto> messageCaptor = ArgumentCaptor.forClass(ChatStreamDto.class);
      verify(messagingTemplate)
          .convertAndSendToUser(eq("testUser"), eq("/sub/chat/1"), messageCaptor.capture());

      ChatStreamDto message = messageCaptor.getValue();
      assertThat(message.message()).isEqualTo("여행 일정 생성이 완료되었습니다");
      assertThat(message.isComplete()).isTrue();
      assertThat(message.messageStreamType()).isEqualTo(MessageStreamType.COMPLETED);
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
      ArgumentCaptor<ErrorResponse> messageCaptor = ArgumentCaptor.forClass(ErrorResponse.class);
      verify(messagingTemplate)
          .convertAndSendToUser(eq("testUser"), eq("/sub/errors/1"), messageCaptor.capture());

      ErrorResponse message = messageCaptor.getValue();
      assertThat(message.message())
          .isEqualTo(AiErrorCode.TRIP_PLAN_GENERATION_ERROR.getDefaultMessage());
      assertThat(message.code()).isEqualTo(AiErrorCode.TRIP_PLAN_GENERATION_ERROR.getCode());
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
      ArgumentCaptor<ChatStreamDto> messageCaptor = ArgumentCaptor.forClass(ChatStreamDto.class);
      verify(messagingTemplate)
          .convertAndSendToUser(eq("testUser"), eq("/sub/chat/1"), messageCaptor.capture());

      ChatStreamDto message = messageCaptor.getValue();
      assertThat(message.message()).contains("사용자 요청");
      assertThat(message.isComplete()).isTrue();
      assertThat(message.messageStreamType()).isEqualTo(MessageStreamType.CANCELLED);
    }
  }

  @Nested
  @DisplayName("일정 수정 시작 이벤트를 수신할 때")
  class HandleTripPlanUpdateStartedTests {

    @Test
    @DisplayName("WebSocket을 통해 일정 채널로 수정 시작 메시지를 전송한다")
    void WebSocket으로_일정_채널에_수정_시작_메시지_전송() {
      // given
      String username = "testUser";

      TripPlanUpdateStartedEvent event =
          TripPlanUpdateStartedEvent.builder()
              .chatRoomId(1L)
              .tripPlanId(100L)
              .username(username)
              .message("일정 수정을 시작합니다")
              .build();

      // when
      tripPlanNotificationListener.handleTripPlanUpdateStarted(event);

      // then
      ArgumentCaptor<TripPlanProgressMessage> messageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate)
          .convertAndSendToUser(eq(username), eq("/sub/schedule/100"), messageCaptor.capture());

      TripPlanProgressMessage message = messageCaptor.getValue();
      assertThat(message.type()).isEqualTo(TripPlanProgressMessage.MessageType.UPDATE_STARTED);
      assertThat(message.tripPlanId()).isEqualTo(100L);
      assertThat(message.message()).isEqualTo("일정 수정을 시작합니다");
    }
  }

  @Nested
  @DisplayName("일정 수정 완료 이벤트를 수신할 때")
  class HandleTripPlanUpdatedTests {

    @Test
    @DisplayName("WebSocket을 통해 일정 채널과 채팅 채널로 수정된 데이터를 전송한다")
    void WebSocket으로_일정_수정_완료_데이터_전송() {
      // given
      DailyPlanResponse dailyPlan =
          DailyPlanResponse.builder()
              .dayNumber(2)
              .date("2025-12-02")
              .scheduledPlaces(Collections.emptyList())
              .build();

      String username = "testUser";

      TripPlanUpdatedEvent event =
          TripPlanUpdatedEvent.builder()
              .chatRoomId(1L)
              .tripPlanId(100L)
              .username(username)
              .dailyPlan(dailyPlan)
              .build();

      // when
      tripPlanNotificationListener.handleTripPlanUpdated(event);

      // then - 일정 채널로 TripPlanProgressMessage 전송 검증
      ArgumentCaptor<TripPlanProgressMessage> tripMessageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate)
          .convertAndSendToUser(
              eq("testUser"), eq("/sub/schedule/100"), tripMessageCaptor.capture());

      TripPlanProgressMessage tripMessage = tripMessageCaptor.getValue();
      assertThat(tripMessage.type()).isEqualTo(TripPlanProgressMessage.MessageType.UPDATED);
      assertThat(tripMessage.tripPlanId()).isEqualTo(100L);
      assertThat(tripMessage.dailyPlan()).isEqualTo(dailyPlan);
      assertThat(tripMessage.message()).contains("2일차 일정이 수정되었습니다.");

      // then - 채팅 채널로 ChatStreamDto 전송 검증
      ArgumentCaptor<ChatStreamDto> chatMessageCaptor =
          ArgumentCaptor.forClass(ChatStreamDto.class);
      verify(messagingTemplate)
          .convertAndSendToUser(eq("testUser"), eq("/sub/chat/1"), chatMessageCaptor.capture());

      ChatStreamDto chatMessage = chatMessageCaptor.getValue();
      assertThat(chatMessage.message()).contains("2일차 일정이 수정되었습니다.");
      assertThat(chatMessage.isComplete()).isTrue();
      assertThat(chatMessage.messageStreamType()).isEqualTo(MessageStreamType.UPDATED);
    }
  }
}
