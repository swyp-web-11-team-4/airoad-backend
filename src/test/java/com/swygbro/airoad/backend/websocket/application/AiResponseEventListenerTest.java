package com.swygbro.airoad.backend.websocket.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.websocket.domain.dto.AiResponseContentType;
import com.swygbro.airoad.backend.websocket.domain.event.AiResponseReceivedEvent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

/**
 * AiResponseEventListener 테스트
 *
 * <p>AI 응답 수신 이벤트 처리 및 WebSocket 전송 로직을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("AiResponseEventListener 클래스")
class AiResponseEventListenerTest {

  @Mock private SimpMessagingTemplate messagingTemplate;

  @InjectMocks private AiResponseEventListener eventListener;

  @Nested
  @DisplayName("handleAiResponseReceived 메서드는")
  class HandleAiResponseReceived {

    @Test
    @DisplayName("CHAT 타입 응답을 채팅 채널로 전송한다")
    void shouldSendChatResponseToCorrectChannel() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 10L;
      String userId = "user123@example.com";
      String content = "서울 3박 4일 여행 일정을 생성했습니다.";
      String expectedDestination = "/sub/chat/" + chatRoomId;
      AiResponseReceivedEvent event =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, content, AiResponseContentType.CHAT, false);

      // when
      eventListener.handleAiResponseReceived(event);

      // then
      then(messagingTemplate).should().convertAndSendToUser(userId, expectedDestination, content);
    }

    @Test
    @DisplayName("SCHEDULE 타입 응답을 일정 채널로 전송한다")
    void shouldSendScheduleResponseToCorrectChannel() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 20L;
      String userId = "user123@example.com";
      String content = "{\"day\": 1, \"activities\": [...]}";
      String expectedDestination = "/sub/schedule/" + tripPlanId;
      AiResponseReceivedEvent event =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, content, AiResponseContentType.SCHEDULE, false);

      // when
      eventListener.handleAiResponseReceived(event);

      // then
      then(messagingTemplate).should().convertAndSendToUser(userId, expectedDestination, content);
    }

    @Test
    @DisplayName("WebSocket 전송 실패 시 에러 메시지를 클라이언트에게 전송한다")
    void shouldSendErrorMessageWhenWebSocketFails() {
      // given
      Long chatRoomId = 2L;
      Long tripPlanId = 20L;
      String userId = "user456@example.com";
      String content = "부산 여행 일정입니다.";
      AiResponseReceivedEvent event =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, content, AiResponseContentType.CHAT, false);

      String expectedDestination = "/sub/chat/" + chatRoomId;
      String expectedErrorDestination = "/sub/errors/" + chatRoomId;

      willThrow(new RuntimeException("WebSocket 연결 실패"))
          .given(messagingTemplate)
          .convertAndSendToUser(userId, expectedDestination, content);

      // when
      eventListener.handleAiResponseReceived(event);

      // then
      then(messagingTemplate).should().convertAndSendToUser(userId, expectedDestination, content);
      then(messagingTemplate)
          .should()
          .convertAndSendToUser(eq(userId), eq(expectedErrorDestination), any());
    }

    @Test
    @DisplayName("스트리밍 청크를 순차적으로 전송한다")
    void shouldSendStreamingChunksSequentially() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 10L;
      String userId = "user123@example.com";
      String expectedDestination = "/sub/chat/" + chatRoomId;

      AiResponseReceivedEvent chunk1 =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, "서울", AiResponseContentType.CHAT, false);
      AiResponseReceivedEvent chunk2 =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, " 3박", AiResponseContentType.CHAT, false);
      AiResponseReceivedEvent chunk3 =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, " 4일 여행", AiResponseContentType.CHAT, false);
      AiResponseReceivedEvent complete =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, " 일정입니다.", AiResponseContentType.CHAT, true);

      // when
      eventListener.handleAiResponseReceived(chunk1);
      eventListener.handleAiResponseReceived(chunk2);
      eventListener.handleAiResponseReceived(chunk3);
      eventListener.handleAiResponseReceived(complete);

      // then
      then(messagingTemplate)
          .should(times(1))
          .convertAndSendToUser(userId, expectedDestination, "서울");
      then(messagingTemplate)
          .should(times(1))
          .convertAndSendToUser(userId, expectedDestination, " 3박");
      then(messagingTemplate)
          .should(times(1))
          .convertAndSendToUser(userId, expectedDestination, " 4일 여행");
      then(messagingTemplate)
          .should(times(1))
          .convertAndSendToUser(userId, expectedDestination, " 일정입니다.");
    }

    @Test
    @DisplayName("여러 사용자의 응답을 독립적으로 처리한다")
    void shouldHandleMultipleUsersIndependently() {
      // given
      AiResponseReceivedEvent event1 =
          new AiResponseReceivedEvent(
              1L, 10L, "user1@example.com", "첫 번째 응답", AiResponseContentType.CHAT, false);
      AiResponseReceivedEvent event2 =
          new AiResponseReceivedEvent(
              2L, 20L, "user2@example.com", "두 번째 응답", AiResponseContentType.CHAT, false);

      // when
      eventListener.handleAiResponseReceived(event1);
      eventListener.handleAiResponseReceived(event2);

      // then
      then(messagingTemplate)
          .should(times(1))
          .convertAndSendToUser("user1@example.com", "/sub/chat/1", "첫 번째 응답");
      then(messagingTemplate)
          .should(times(1))
          .convertAndSendToUser("user2@example.com", "/sub/chat/2", "두 번째 응답");
    }
  }
}
