package com.swygbro.airoad.backend.chat.presentation.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class WebSocketEventListenerTest {

  @InjectMocks private WebSocketEventListener listener;

  @Nested
  @DisplayName("사용자가 WebSocket에 연결할 때")
  class HandleWebSocketConnect {

    @Test
    @DisplayName("연결 시도 이벤트를 처리한다")
    void 연결_시도_이벤트_처리() {
      // given
      StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
      headerAccessor.setSessionId("session-user-1");
      Message<byte[]> message =
          MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());
      SessionConnectEvent event = new SessionConnectEvent(this, message);

      // when & then
      assertThatCode(() -> listener.handleWebSocketConnectListener(event))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("연결 완료 이벤트를 처리한다")
    void 연결_완료_이벤트_처리() {
      // given
      StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
      headerAccessor.setSessionId("session-user-1");
      Message<byte[]> message =
          MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());
      SessionConnectedEvent event = new SessionConnectedEvent(this, message);

      // when & then
      assertThatCode(() -> listener.handleWebSocketConnectedListener(event))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("사용자가 자신의 채팅방을 구독할 때")
  class HandleWebSocketSubscribe {

    @Test
    @DisplayName("여행 일정 진행 상황 토픽을 구독한다")
    void 여행_일정_진행_상황_구독() {
      // given
      StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      headerAccessor.setSessionId("session-user-1");
      headerAccessor.setDestination("/sub/chatroom/100/trip-progress");
      headerAccessor.setSubscriptionId("sub-1");
      Message<byte[]> message =
          MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());
      SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

      // when & then
      assertThatCode(() -> listener.handleWebSocketSubscribeListener(event))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("사용자가 WebSocket 연결을 종료할 때")
  class HandleWebSocketDisconnect {

    @Test
    @DisplayName("정상 종료로 연결을 해제한다")
    void 정상_종료() {
      // given
      StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
      headerAccessor.setSessionId("session-user-1");
      Message<byte[]> message =
          MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());
      SessionDisconnectEvent event =
          new SessionDisconnectEvent(this, message, "session-user-1", CloseStatus.NORMAL);

      // when & then
      assertThatCode(() -> listener.handleWebSocketDisconnectListener(event))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("네트워크 오류로 연결이 끊어진다")
    void 네트워크_오류로_연결_끊김() {
      // given
      StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
      headerAccessor.setSessionId("session-user-1");
      Message<byte[]> message =
          MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());
      SessionDisconnectEvent event =
          new SessionDisconnectEvent(
              this, message, "session-user-1", CloseStatus.SESSION_NOT_RELIABLE);

      // when & then
      assertThatCode(() -> listener.handleWebSocketDisconnectListener(event))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("브라우저 닫기로 연결이 끊어진다")
    void 브라우저_닫기로_연결_끊김() {
      // given
      StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
      headerAccessor.setSessionId("session-user-1");
      Message<byte[]> message =
          MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());
      SessionDisconnectEvent event =
          new SessionDisconnectEvent(this, message, "session-user-1", CloseStatus.GOING_AWAY);

      // when & then
      assertThatCode(() -> listener.handleWebSocketDisconnectListener(event))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("사용자의 WebSocket 세션 전체 흐름")
  class WebSocketSessionLifecycle {

    @Test
    @DisplayName("연결 후 여행 일정 진행 상황을 구독하고 정상 종료한다")
    void 정상적인_세션_흐름() {
      // given
      String sessionId = "session-user-1";
      Long chatRoomId = 100L;

      SessionConnectEvent connectEvent = createConnectEvent(sessionId);
      SessionConnectedEvent connectedEvent = createConnectedEvent(sessionId);
      SessionSubscribeEvent subscribeEvent =
          createSubscribeEvent(sessionId, "/sub/chatroom/" + chatRoomId + "/trip-progress");
      SessionDisconnectEvent disconnectEvent = createDisconnectEvent(sessionId, CloseStatus.NORMAL);

      // when & then
      assertThatCode(
              () -> {
                listener.handleWebSocketConnectListener(connectEvent);
                listener.handleWebSocketConnectedListener(connectedEvent);
                listener.handleWebSocketSubscribeListener(subscribeEvent);
                listener.handleWebSocketDisconnectListener(disconnectEvent);
              })
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("연결 후 네트워크 오류로 비정상 종료한다")
    void 네트워크_오류로_비정상_종료() {
      // given
      String sessionId = "session-user-1";
      Long chatRoomId = 100L;

      SessionConnectEvent connectEvent = createConnectEvent(sessionId);
      SessionConnectedEvent connectedEvent = createConnectedEvent(sessionId);
      SessionSubscribeEvent subscribeEvent =
          createSubscribeEvent(sessionId, "/sub/chatroom/" + chatRoomId + "/trip-progress");
      SessionDisconnectEvent disconnectEvent =
          createDisconnectEvent(sessionId, CloseStatus.SESSION_NOT_RELIABLE);

      // when & then
      assertThatCode(
              () -> {
                listener.handleWebSocketConnectListener(connectEvent);
                listener.handleWebSocketConnectedListener(connectedEvent);
                listener.handleWebSocketSubscribeListener(subscribeEvent);
                listener.handleWebSocketDisconnectListener(disconnectEvent);
              })
          .doesNotThrowAnyException();
    }

    private SessionConnectEvent createConnectEvent(String sessionId) {
      StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
      headerAccessor.setSessionId(sessionId);
      Message<byte[]> message =
          MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());
      return new SessionConnectEvent(this, message);
    }

    private SessionConnectedEvent createConnectedEvent(String sessionId) {
      StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
      headerAccessor.setSessionId(sessionId);
      Message<byte[]> message =
          MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());
      return new SessionConnectedEvent(this, message);
    }

    private SessionSubscribeEvent createSubscribeEvent(String sessionId, String destination) {
      StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      headerAccessor.setSessionId(sessionId);
      headerAccessor.setDestination(destination);
      headerAccessor.setSubscriptionId("sub-" + sessionId);
      Message<byte[]> message =
          MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());
      return new SessionSubscribeEvent(this, message);
    }

    private SessionDisconnectEvent createDisconnectEvent(String sessionId, CloseStatus status) {
      StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
      headerAccessor.setSessionId(sessionId);
      Message<byte[]> message =
          MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());
      return new SessionDisconnectEvent(this, message, sessionId, status);
    }
  }
}
