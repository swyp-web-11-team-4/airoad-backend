package com.swygbro.airoad.backend.chat.presentation.web;

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
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * WebSocketPayloadTypeInterceptor 테스트
 *
 * <p>WebSocket STOMP 메시지의 Content-Type을 자동으로 설정하는 인터셉터의 동작을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("WebSocketPayloadTypeInterceptor")
class WebSocketPayloadTypeInterceptorTest {

  @InjectMocks private WebSocketPayloadTypeInterceptor interceptor;

  @Nested
  @DisplayName("SEND 명령 처리")
  class SendCommand {

    @Test
    @DisplayName("Content-Type이 없는 SEND 메시지에 application/json을 설정한다")
    void shouldSetApplicationJsonWhenContentTypeIsNull() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
      accessor.setLeaveMutable(true);
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();

      StompHeaderAccessor resultAccessor =
          MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
      assertThat(resultAccessor).isNotNull();
      assertThat(resultAccessor.getContentType()).isEqualTo(MimeTypeUtils.APPLICATION_JSON);
    }

    @Test
    @DisplayName("Content-Type이 이미 설정된 SEND 메시지는 변경하지 않는다")
    void shouldNotChangeExistingContentType() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
      MimeType customContentType = MimeTypeUtils.TEXT_PLAIN;
      accessor.setContentType(customContentType);
      accessor.setLeaveMutable(true);
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();

      StompHeaderAccessor resultAccessor =
          MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
      assertThat(resultAccessor).isNotNull();
      assertThat(resultAccessor.getContentType()).isEqualTo(customContentType);
    }
  }

  @Nested
  @DisplayName("기타 STOMP 명령")
  class OtherStompCommands {

    @Test
    @DisplayName("CONNECT 명령은 Content-Type을 설정하지 않는다")
    void shouldNotSetContentTypeForConnectCommand() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
      accessor.setLeaveMutable(true);
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();

      StompHeaderAccessor resultAccessor =
          MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
      assertThat(resultAccessor).isNotNull();
      assertThat(resultAccessor.getContentType()).isNull();
    }

    @Test
    @DisplayName("SUBSCRIBE 명령은 Content-Type을 설정하지 않는다")
    void shouldNotSetContentTypeForSubscribeCommand() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      accessor.setDestination("/topic/test");
      accessor.setLeaveMutable(true);
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();

      StompHeaderAccessor resultAccessor =
          MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
      assertThat(resultAccessor).isNotNull();
      assertThat(resultAccessor.getContentType()).isNull();
    }

    @Test
    @DisplayName("DISCONNECT 명령은 Content-Type을 설정하지 않는다")
    void shouldNotSetContentTypeForDisconnectCommand() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
      accessor.setLeaveMutable(true);
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();

      StompHeaderAccessor resultAccessor =
          MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
      assertThat(resultAccessor).isNotNull();
      assertThat(resultAccessor.getContentType()).isNull();
    }
  }

  @Nested
  @DisplayName("Accessor 예외 처리")
  class AccessorExceptionHandling {

    @Test
    @DisplayName("Accessor가 null인 경우 원본 메시지를 그대로 반환한다")
    void shouldReturnOriginalMessageWhenAccessorIsNull() {
      // given
      Message<?> message = MessageBuilder.withPayload(new byte[0]).build();

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();
      assertThat(result).isEqualTo(message);
    }
  }
}
