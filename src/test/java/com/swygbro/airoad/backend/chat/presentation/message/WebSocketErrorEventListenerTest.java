package com.swygbro.airoad.backend.chat.presentation.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.common.domain.dto.ErrorResponse;
import com.swygbro.airoad.backend.common.domain.event.WebSocketErrorEvent;

import static org.mockito.BDDMockito.*;

/**
 * WebSocketErrorEventListener 테스트
 *
 * <p>WebSocket 에러 이벤트 리스너의 동작을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("WebSocketErrorEventListener")
class WebSocketErrorEventListenerTest {

  @Mock private SimpMessagingTemplate messagingTemplate;

  @InjectMocks private WebSocketErrorEventListener listener;

  private static final String USER_ID = "user@example.com";
  private static final String ERROR_CHANNEL = "/sub/errors/123";
  private static final String ERROR_CODE = "WS001";
  private static final String ERROR_MESSAGE = "WebSocket 에러 발생";

  @Nested
  @DisplayName("handleWebSocketError 메서드는")
  class HandleWebSocketError {

    @Test
    @DisplayName("WebSocketErrorEvent를 받아 SimpMessagingTemplate로 에러를 전송한다")
    void shouldSendErrorMessageToUser() {
      // given
      ErrorResponse errorResponse = ErrorResponse.of(ERROR_CODE, ERROR_MESSAGE, "/pub/chat/123");
      WebSocketErrorEvent event = new WebSocketErrorEvent(USER_ID, ERROR_CHANNEL, errorResponse);

      // when
      listener.handleWebSocketError(event);

      // then
      verify(messagingTemplate).convertAndSendToUser(USER_ID, ERROR_CHANNEL, errorResponse);
    }

    @Test
    @DisplayName("여러 WebSocketErrorEvent를 순차적으로 처리한다")
    void shouldHandleMultipleEvents() {
      // given
      ErrorResponse errorResponse1 = ErrorResponse.of("WS001", "첫 번째 에러", "/pub/chat/123");
      ErrorResponse errorResponse2 = ErrorResponse.of("WS002", "두 번째 에러", "/pub/chat/456");

      WebSocketErrorEvent event1 =
          new WebSocketErrorEvent("user1@example.com", "/sub/errors/123", errorResponse1);
      WebSocketErrorEvent event2 =
          new WebSocketErrorEvent("user2@example.com", "/sub/errors/456", errorResponse2);

      // when
      listener.handleWebSocketError(event1);
      listener.handleWebSocketError(event2);

      // then
      verify(messagingTemplate)
          .convertAndSendToUser("user1@example.com", "/sub/errors/123", errorResponse1);
      verify(messagingTemplate)
          .convertAndSendToUser("user2@example.com", "/sub/errors/456", errorResponse2);
    }

    @Test
    @DisplayName("SimpMessagingTemplate에서 예외 발생 시에도 실패하지 않는다")
    void shouldNotFailWhenMessagingTemplateFails() {
      // given
      ErrorResponse errorResponse = ErrorResponse.of(ERROR_CODE, ERROR_MESSAGE, "/pub/chat/123");
      WebSocketErrorEvent event = new WebSocketErrorEvent(USER_ID, ERROR_CHANNEL, errorResponse);

      willThrow(new RuntimeException("메시징 실패"))
          .given(messagingTemplate)
          .convertAndSendToUser(any(), any(), any());

      // when & then - 예외가 전파되지 않아야 함
      listener.handleWebSocketError(event);

      verify(messagingTemplate).convertAndSendToUser(USER_ID, ERROR_CHANNEL, errorResponse);
    }

    @Test
    @DisplayName("unknown 채널로 에러를 전송할 수 있다")
    void shouldSendErrorToUnknownChannel() {
      // given
      ErrorResponse errorResponse =
          ErrorResponse.of(ERROR_CODE, ERROR_MESSAGE, "/pub/other/message");
      WebSocketErrorEvent event =
          new WebSocketErrorEvent(USER_ID, "/sub/errors/unknown", errorResponse);

      // when
      listener.handleWebSocketError(event);

      // then
      verify(messagingTemplate).convertAndSendToUser(USER_ID, "/sub/errors/unknown", errorResponse);
    }
  }
}
