package com.swygbro.airoad.backend.trip.presentation.message;

import java.time.LocalDate;
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

import com.swygbro.airoad.backend.common.exception.CommonErrorCode;
import com.swygbro.airoad.backend.trip.domain.dto.DailyPlanDto;
import com.swygbro.airoad.backend.trip.domain.dto.TripPlanProgressMessage;
import com.swygbro.airoad.backend.trip.domain.event.DailyPlanGeneratedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationCancelledEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationCompletedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationErrorEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * TripPlanProgressListener의 테스트 클래스입니다.
 *
 * <p>여행 일정 생성 진행 상황 이벤트를 수신하여 WebSocket 메시지로 전송하는 기능을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("TripPlanProgressListener 클래스")
class TripPlanProgressListenerTest {

  @Mock private SimpMessagingTemplate messagingTemplate;

  @InjectMocks private TripPlanProgressListener listener;

  // 테스트 데이터 상수
  private static final String TEST_SESSION_ID = "test-session-id";
  private static final Long TEST_CHAT_ROOM_ID = 1L;
  private static final Long TEST_TRIP_PLAN_ID = 100L;
  private static final String TEST_DESTINATION = "/sub/chatroom/1/trip-progress";

  /**
   * 테스트용 DailyPlanDto를 생성하는 헬퍼 메서드입니다.
   *
   * @param dayNumber 일차
   * @return 테스트용 DailyPlanDto
   */
  private DailyPlanDto createTestDailyPlan(int dayNumber) {
    return DailyPlanDto.builder()
        .dayNumber(dayNumber)
        .date(LocalDate.of(2024, 3, dayNumber))
        .places(Collections.emptyList())
        .build();
  }

  @Nested
  @DisplayName("handleDailyPlanGenerated 메서드는")
  class HandleDailyPlanGenerated {

    @Test
    @DisplayName("일차별 일정 생성 이벤트를 받아 WebSocket 메시지를 전송한다")
    void shouldSendWebSocketMessageWhenDailyPlanGenerated() {
      // given - 1일차 일정 생성 이벤트 준비
      DailyPlanDto dailyPlan = createTestDailyPlan(1);
      DailyPlanGeneratedEvent event =
          new DailyPlanGeneratedEvent(
              this, TEST_SESSION_ID, TEST_CHAT_ROOM_ID, TEST_TRIP_PLAN_ID, dailyPlan);

      // when - 이벤트 처리
      listener.handleDailyPlanGenerated(event);

      // then - WebSocket 메시지 전송 검증
      ArgumentCaptor<TripPlanProgressMessage> messageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate).convertAndSend(eq(TEST_DESTINATION), messageCaptor.capture());

      TripPlanProgressMessage sentMessage = messageCaptor.getValue();
      assertThat(sentMessage.type())
          .isEqualTo(TripPlanProgressMessage.MessageType.DAILY_PLAN_GENERATED);
      assertThat(sentMessage.tripPlanId()).isEqualTo(TEST_TRIP_PLAN_ID);
      assertThat(sentMessage.dailyPlan()).isEqualTo(dailyPlan);
      assertThat(sentMessage.message()).isEqualTo("1일차 일정이 생성되었습니다.");
    }

    @Test
    @DisplayName("2일차 이상의 일정 생성 이벤트도 정상적으로 처리한다")
    void shouldHandleMultipleDayNumbers() {
      // given - 3일차 일정 생성 이벤트 준비
      DailyPlanDto dailyPlan = createTestDailyPlan(3);
      DailyPlanGeneratedEvent event =
          new DailyPlanGeneratedEvent(
              this, TEST_SESSION_ID, TEST_CHAT_ROOM_ID, TEST_TRIP_PLAN_ID, dailyPlan);

      // when - 이벤트 처리
      listener.handleDailyPlanGenerated(event);

      // then - 올바른 일차 메시지가 전송됨
      ArgumentCaptor<TripPlanProgressMessage> messageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate).convertAndSend(eq(TEST_DESTINATION), messageCaptor.capture());

      TripPlanProgressMessage sentMessage = messageCaptor.getValue();
      assertThat(sentMessage.dailyPlan().dayNumber()).isEqualTo(3);
      assertThat(sentMessage.message()).isEqualTo("3일차 일정이 생성되었습니다.");
    }

    @Test
    @DisplayName("올바른 destination으로 메시지를 전송한다")
    void shouldSendToCorrectDestination() {
      // given - 일차별 일정 생성 이벤트 준비
      DailyPlanDto dailyPlan = createTestDailyPlan(1);
      DailyPlanGeneratedEvent event =
          new DailyPlanGeneratedEvent(
              this, TEST_SESSION_ID, TEST_CHAT_ROOM_ID, TEST_TRIP_PLAN_ID, dailyPlan);

      // when - 이벤트 처리
      listener.handleDailyPlanGenerated(event);

      // then - 올바른 destination으로 전송됨
      verify(messagingTemplate)
          .convertAndSend(eq(TEST_DESTINATION), any(TripPlanProgressMessage.class));
    }
  }

  @Nested
  @DisplayName("handleTripPlanGenerationCompleted 메서드는")
  class HandleTripPlanGenerationCompleted {

    @Test
    @DisplayName("전체 일정 생성 완료 이벤트를 받아 WebSocket 메시지를 전송한다")
    void shouldSendWebSocketMessageWhenGenerationCompleted() {
      // given - 전체 일정 생성 완료 이벤트 준비
      String completionMessage = "여행 일정이 모두 생성되었습니다.";
      TripPlanGenerationCompletedEvent event =
          new TripPlanGenerationCompletedEvent(
              this, TEST_SESSION_ID, TEST_CHAT_ROOM_ID, TEST_TRIP_PLAN_ID, completionMessage);

      // when - 이벤트 처리
      listener.handleTripPlanGenerationCompleted(event);

      // then - WebSocket 메시지 전송 검증
      ArgumentCaptor<TripPlanProgressMessage> messageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate).convertAndSend(eq(TEST_DESTINATION), messageCaptor.capture());

      TripPlanProgressMessage sentMessage = messageCaptor.getValue();
      assertThat(sentMessage.type()).isEqualTo(TripPlanProgressMessage.MessageType.COMPLETED);
      assertThat(sentMessage.tripPlanId()).isEqualTo(TEST_TRIP_PLAN_ID);
      assertThat(sentMessage.message()).isEqualTo(completionMessage);
      assertThat(sentMessage.dailyPlan()).isNull();
      assertThat(sentMessage.errorCode()).isNull();
    }

    @Test
    @DisplayName("완료 메시지가 정확히 전달된다")
    void shouldTransmitCompletionMessageAccurately() {
      // given - 커스텀 완료 메시지와 함께 이벤트 준비
      String customMessage = "3박 4일 제주도 여행 일정이 완성되었습니다!";
      TripPlanGenerationCompletedEvent event =
          new TripPlanGenerationCompletedEvent(
              this, TEST_SESSION_ID, TEST_CHAT_ROOM_ID, TEST_TRIP_PLAN_ID, customMessage);

      // when - 이벤트 처리
      listener.handleTripPlanGenerationCompleted(event);

      // then - 커스텀 메시지가 그대로 전송됨
      ArgumentCaptor<TripPlanProgressMessage> messageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate).convertAndSend(eq(TEST_DESTINATION), messageCaptor.capture());

      assertThat(messageCaptor.getValue().message()).isEqualTo(customMessage);
    }
  }

  @Nested
  @DisplayName("handleTripPlanGenerationError 메서드는")
  class HandleTripPlanGenerationError {

    @Test
    @DisplayName("일정 생성 오류 이벤트를 받아 WebSocket 에러 메시지를 전송한다")
    void shouldSendWebSocketErrorMessageWhenGenerationFails() {
      // given - 일정 생성 오류 이벤트 준비
      String errorMessage = "AI 서비스 연결에 실패했습니다.";
      Throwable exception = new RuntimeException("Connection timeout");
      TripPlanGenerationErrorEvent event =
          new TripPlanGenerationErrorEvent(
              this,
              TEST_SESSION_ID,
              TEST_CHAT_ROOM_ID,
              TEST_TRIP_PLAN_ID,
              CommonErrorCode.INTERNAL_ERROR,
              errorMessage,
              exception);

      // when - 이벤트 처리
      listener.handleTripPlanGenerationError(event);

      // then - WebSocket 에러 메시지 전송 검증
      ArgumentCaptor<TripPlanProgressMessage> messageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate).convertAndSend(eq(TEST_DESTINATION), messageCaptor.capture());

      TripPlanProgressMessage sentMessage = messageCaptor.getValue();
      assertThat(sentMessage.type()).isEqualTo(TripPlanProgressMessage.MessageType.ERROR);
      assertThat(sentMessage.tripPlanId()).isEqualTo(TEST_TRIP_PLAN_ID);
      assertThat(sentMessage.message()).isEqualTo(errorMessage);
      assertThat(sentMessage.errorCode()).isEqualTo(CommonErrorCode.INTERNAL_ERROR.getCode());
      assertThat(sentMessage.dailyPlan()).isNull();
    }

    @Test
    @DisplayName("tripPlanId가 null인 경우에도 오류 메시지를 전송한다")
    void shouldHandleNullTripPlanId() {
      // given - tripPlanId가 null인 오류 이벤트 준비
      String errorMessage = "일정 생성을 시작하지 못했습니다.";
      TripPlanGenerationErrorEvent event =
          new TripPlanGenerationErrorEvent(
              this,
              TEST_SESSION_ID,
              TEST_CHAT_ROOM_ID,
              null, // tripPlanId가 null
              CommonErrorCode.INTERNAL_ERROR,
              errorMessage,
              null);

      // when - 이벤트 처리
      listener.handleTripPlanGenerationError(event);

      // then - tripPlanId가 null이어도 메시지 전송됨
      ArgumentCaptor<TripPlanProgressMessage> messageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate).convertAndSend(eq(TEST_DESTINATION), messageCaptor.capture());

      assertThat(messageCaptor.getValue().tripPlanId()).isNull();
      assertThat(messageCaptor.getValue().message()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("다양한 ErrorCode를 올바르게 처리한다")
    void shouldHandleDifferentErrorCodes() {
      // given - INVALID_INPUT 에러 코드를 가진 이벤트 준비
      String errorMessage = "잘못된 입력값입니다.";
      TripPlanGenerationErrorEvent event =
          new TripPlanGenerationErrorEvent(
              this,
              TEST_SESSION_ID,
              TEST_CHAT_ROOM_ID,
              TEST_TRIP_PLAN_ID,
              CommonErrorCode.INVALID_INPUT,
              errorMessage,
              null);

      // when - 이벤트 처리
      listener.handleTripPlanGenerationError(event);

      // then - 올바른 에러 코드가 전송됨
      ArgumentCaptor<TripPlanProgressMessage> messageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate).convertAndSend(eq(TEST_DESTINATION), messageCaptor.capture());

      assertThat(messageCaptor.getValue().errorCode())
          .isEqualTo(CommonErrorCode.INVALID_INPUT.getCode());
    }
  }

  @Nested
  @DisplayName("handleTripPlanGenerationCancelled 메서드는")
  class HandleTripPlanGenerationCancelled {

    @Test
    @DisplayName("일정 생성 취소 이벤트를 받아 WebSocket 취소 메시지를 전송한다")
    void shouldSendWebSocketCancelMessageWhenGenerationCancelled() {
      // given - 일정 생성 취소 이벤트 준비
      String cancelReason = "사용자가 생성을 중단했습니다.";
      TripPlanGenerationCancelledEvent event =
          new TripPlanGenerationCancelledEvent(
              this, TEST_SESSION_ID, TEST_CHAT_ROOM_ID, TEST_TRIP_PLAN_ID, cancelReason);

      // when - 이벤트 처리
      listener.handleTripPlanGenerationCancelled(event);

      // then - WebSocket 취소 메시지 전송 검증
      ArgumentCaptor<TripPlanProgressMessage> messageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate).convertAndSend(eq(TEST_DESTINATION), messageCaptor.capture());

      TripPlanProgressMessage sentMessage = messageCaptor.getValue();
      assertThat(sentMessage.type()).isEqualTo(TripPlanProgressMessage.MessageType.CANCELLED);
      assertThat(sentMessage.tripPlanId()).isEqualTo(TEST_TRIP_PLAN_ID);
      assertThat(sentMessage.message()).isEqualTo("일정 생성이 취소되었습니다: " + cancelReason);
      assertThat(sentMessage.dailyPlan()).isNull();
      assertThat(sentMessage.errorCode()).isNull();
    }

    @Test
    @DisplayName("취소 사유가 메시지에 포함된다")
    void shouldIncludeCancelReasonInMessage() {
      // given - 특정 취소 사유와 함께 이벤트 준비
      String reason = "WebSocket 연결이 끊어졌습니다";
      TripPlanGenerationCancelledEvent event =
          new TripPlanGenerationCancelledEvent(
              this, TEST_SESSION_ID, TEST_CHAT_ROOM_ID, TEST_TRIP_PLAN_ID, reason);

      // when - 이벤트 처리
      listener.handleTripPlanGenerationCancelled(event);

      // then - 취소 사유가 메시지에 포함됨
      ArgumentCaptor<TripPlanProgressMessage> messageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate).convertAndSend(eq(TEST_DESTINATION), messageCaptor.capture());

      assertThat(messageCaptor.getValue().message()).contains(reason);
    }

    @Test
    @DisplayName("tripPlanId가 null인 취소 이벤트도 처리한다")
    void shouldHandleNullTripPlanIdInCancellation() {
      // given - tripPlanId가 null인 취소 이벤트 준비
      String reason = "일정 생성이 시작되기 전에 취소됨";
      TripPlanGenerationCancelledEvent event =
          new TripPlanGenerationCancelledEvent(
              this,
              TEST_SESSION_ID,
              TEST_CHAT_ROOM_ID,
              null, // tripPlanId가 null
              reason);

      // when - 이벤트 처리
      listener.handleTripPlanGenerationCancelled(event);

      // then - tripPlanId가 null이어도 메시지 전송됨
      ArgumentCaptor<TripPlanProgressMessage> messageCaptor =
          ArgumentCaptor.forClass(TripPlanProgressMessage.class);
      verify(messagingTemplate).convertAndSend(eq(TEST_DESTINATION), messageCaptor.capture());

      assertThat(messageCaptor.getValue().tripPlanId()).isNull();
      assertThat(messageCaptor.getValue().type())
          .isEqualTo(TripPlanProgressMessage.MessageType.CANCELLED);
    }
  }

  @Nested
  @DisplayName("WebSocket 메시지 전송 시")
  class WebSocketMessageSending {

    @Test
    @DisplayName("chatRoomId를 기반으로 올바른 destination을 생성한다")
    void shouldGenerateCorrectDestinationFromChatRoomId() {
      // given - 특정 chatRoomId를 가진 이벤트 준비
      Long specificChatRoomId = 999L;
      DailyPlanDto dailyPlan = createTestDailyPlan(1);
      DailyPlanGeneratedEvent event =
          new DailyPlanGeneratedEvent(
              this, TEST_SESSION_ID, specificChatRoomId, TEST_TRIP_PLAN_ID, dailyPlan);

      // when - 이벤트 처리
      listener.handleDailyPlanGenerated(event);

      // then - chatRoomId가 destination에 포함됨
      String expectedDestination = "/sub/chatroom/999/trip-progress";
      verify(messagingTemplate)
          .convertAndSend(eq(expectedDestination), any(TripPlanProgressMessage.class));
    }
  }
}
