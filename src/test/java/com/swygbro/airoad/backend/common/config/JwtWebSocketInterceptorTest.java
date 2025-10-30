package com.swygbro.airoad.backend.common.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.swygbro.airoad.backend.auth.application.UserDetailsServiceImpl;
import com.swygbro.airoad.backend.auth.domain.entity.TokenType;
import com.swygbro.airoad.backend.auth.filter.JwtTokenProvider;
import com.swygbro.airoad.backend.chat.config.JwtWebSocketInterceptor;
import com.swygbro.airoad.backend.common.exception.WebSocketErrorCode;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * JwtWebSocketInterceptor 테스트
 *
 * <p>WebSocket STOMP 메시지 인증 및 권한 검증 인터셉터의 동작을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("JwtWebSocketInterceptor")
class JwtWebSocketInterceptorTest {

  @Mock private JwtTokenProvider jwtTokenProvider;

  @Mock private UserDetailsServiceImpl userDetailsService;

  @Mock private SimpMessagingTemplate messagingTemplate;

  @InjectMocks private JwtWebSocketInterceptor interceptor;

  private static final String VALID_TOKEN = "valid.jwt.token";
  private static final String INVALID_TOKEN = "invalid.jwt.token";
  private static final String USER_EMAIL = "user@example.com";

  private UserDetails userDetails;

  @BeforeEach
  void setUp() {
    userDetails =
        new User(USER_EMAIL, "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }

  @Nested
  @DisplayName("CONNECT 명령 처리")
  class ConnectCommand {

    @Test
    @DisplayName("유효한 JWT 토큰으로 연결 시 인증에 성공한다")
    void shouldAuthenticateSuccessfullyWithValidToken() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
      accessor.setNativeHeader("Authorization", "Bearer " + VALID_TOKEN);
      accessor.setLeaveMutable(true); // accessor가 mutable하게 유지되도록 설정
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      willDoNothing().given(jwtTokenProvider).validateAccessToken(VALID_TOKEN);
      given(
              jwtTokenProvider.getClaimFromToken(
                  VALID_TOKEN, "email", String.class, TokenType.ACCESS_TOKEN))
          .willReturn(USER_EMAIL);
      given(userDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();

      // 원본 accessor가 수정되므로 같은 객체를 확인
      StompHeaderAccessor resultAccessor =
          MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
      Authentication authentication = (Authentication) resultAccessor.getUser();
      assertThat(authentication).isNotNull();
      assertThat(authentication.getPrincipal()).isEqualTo(userDetails);

      verify(jwtTokenProvider).validateAccessToken(VALID_TOKEN);
      verify(jwtTokenProvider)
          .getClaimFromToken(VALID_TOKEN, "email", String.class, TokenType.ACCESS_TOKEN);
      verify(userDetailsService).loadUserByUsername(USER_EMAIL);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 STOMP ERROR 프레임을 반환한다")
    void shouldReturnErrorFrameWhenAuthorizationHeaderMissing() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();

      StompHeaderAccessor resultAccessor =
          MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
      assertThat(resultAccessor.getCommand()).isEqualTo(StompCommand.ERROR);
      assertThat(resultAccessor.getMessage())
          .isEqualTo(WebSocketErrorCode.UNAUTHORIZED_CONNECTION.getDefaultMessage());
      assertThat(resultAccessor.getFirstNativeHeader("error-code"))
          .isEqualTo(WebSocketErrorCode.UNAUTHORIZED_CONNECTION.getCode());

      verify(jwtTokenProvider, never()).validateAccessToken(any());
    }

    @Test
    @DisplayName("JWT 검증 실패 시 STOMP ERROR 프레임을 반환한다")
    void shouldReturnErrorFrameWhenJwtValidationFails() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
      accessor.setNativeHeader("Authorization", "Bearer " + INVALID_TOKEN);
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      willThrow(new io.jsonwebtoken.ExpiredJwtException(null, null, "Token expired"))
          .given(jwtTokenProvider)
          .validateAccessToken(INVALID_TOKEN);

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();

      StompHeaderAccessor resultAccessor =
          MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
      assertThat(resultAccessor.getCommand()).isEqualTo(StompCommand.ERROR);
      assertThat(resultAccessor.getFirstNativeHeader("error-code"))
          .isEqualTo(WebSocketErrorCode.UNAUTHORIZED_CONNECTION.getCode());

      verify(jwtTokenProvider).validateAccessToken(INVALID_TOKEN);
      verify(userDetailsService, never()).loadUserByUsername(any());
    }
  }

  @Nested
  @DisplayName("SUBSCRIBE 명령 처리")
  class SubscribeCommand {

    @Test
    @DisplayName("인증된 사용자가 허용된 경로를 구독하면 성공한다")
    void shouldAllowSubscriptionToAllowedPath() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      accessor.setDestination("/user/sub/chat/123");
      accessor.setUser(createAuthentication());
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();
      assertThat(result).isEqualTo(message);
    }

    @Test
    @DisplayName("허용되지 않은 경로 구독 시 STOMP ERROR 프레임을 반환한다")
    void shouldReturnErrorFrameWhenSubscribingToForbiddenPath() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      accessor.setDestination("/topic/public"); // 허용되지 않은 경로
      accessor.setUser(createAuthentication());
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();

      StompHeaderAccessor resultAccessor =
          MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
      assertThat(resultAccessor.getCommand()).isEqualTo(StompCommand.ERROR);
      assertThat(resultAccessor.getFirstNativeHeader("error-code"))
          .isEqualTo(WebSocketErrorCode.FORBIDDEN_SUBSCRIPTION.getCode());
    }
  }

  @Nested
  @DisplayName("SEND 명령 처리")
  class SendCommand {

    @Test
    @DisplayName("인증된 사용자가 허용된 경로로 메시지 전송 시 성공한다")
    void shouldAllowSendingToAllowedPath() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
      accessor.setDestination("/pub/chat/123/message");
      accessor.setUser(createAuthentication());
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();
      assertThat(result).isEqualTo(message);
    }

    @Test
    @DisplayName("허용되지 않은 경로로 메시지 전송 시 에러 채널로 전송하고 null을 반환한다")
    void shouldSendToErrorChannelWhenSendingToForbiddenPath() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
      accessor.setDestination("/pub/other/message"); // 허용되지 않은 경로
      accessor.setUser(createAuthentication());
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNull(); // SEND 에러는 null 반환 (메시지 전달 중단)

      // 에러 채널로 전송 검증
      verify(messagingTemplate)
          .convertAndSendToUser(eq(USER_EMAIL), eq("/sub/errors/unknown"), any());
    }
  }

  @Nested
  @DisplayName("STOMP ERROR 프레임 생성")
  class ErrorFrameCreation {

    @Test
    @DisplayName("UNAUTHORIZED_CONNECTION 에러 시 올바른 STOMP ERROR 프레임을 생성한다")
    void shouldCreateProperErrorFrameForUnauthorizedConnection() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
      accessor.setSessionId("test-session-123");
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();

      StompHeaderAccessor resultAccessor =
          MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);

      // ERROR 프레임 검증
      assertThat(resultAccessor.getCommand()).isEqualTo(StompCommand.ERROR);

      // 에러 메시지 검증
      assertThat(resultAccessor.getMessage())
          .isEqualTo(WebSocketErrorCode.UNAUTHORIZED_CONNECTION.getDefaultMessage());

      // 에러 코드 헤더 검증
      assertThat(resultAccessor.getFirstNativeHeader("error-code"))
          .isEqualTo(WebSocketErrorCode.UNAUTHORIZED_CONNECTION.getCode());

      // 세션 ID 유지 검증
      assertThat(resultAccessor.getSessionId()).isEqualTo("test-session-123");

      // 페이로드 검증 (byte[] 형태로 에러 메시지 포함)
      Object payload = result.getPayload();
      assertThat(payload).isInstanceOf(byte[].class);
      String payloadString = new String((byte[]) payload, StandardCharsets.UTF_8);
      assertThat(payloadString)
          .isEqualTo(WebSocketErrorCode.UNAUTHORIZED_CONNECTION.getDefaultMessage());
    }

    @Test
    @DisplayName("FORBIDDEN_SUBSCRIPTION 에러 시 올바른 STOMP ERROR 프레임을 생성한다")
    void shouldCreateProperErrorFrameForForbiddenSubscription() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      accessor.setDestination("/topic/forbidden");
      accessor.setUser(createAuthentication());
      accessor.setSessionId("test-session-456");
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();

      StompHeaderAccessor resultAccessor =
          MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);

      // ERROR 프레임 검증
      assertThat(resultAccessor.getCommand()).isEqualTo(StompCommand.ERROR);
      assertThat(resultAccessor.getMessage())
          .isEqualTo(WebSocketErrorCode.FORBIDDEN_SUBSCRIPTION.getDefaultMessage());
      assertThat(resultAccessor.getFirstNativeHeader("error-code"))
          .isEqualTo(WebSocketErrorCode.FORBIDDEN_SUBSCRIPTION.getCode());
      assertThat(resultAccessor.getSessionId()).isEqualTo("test-session-456");

      // 페이로드 검증
      Object payload = result.getPayload();
      assertThat(payload).isInstanceOf(byte[].class);
      String payloadString = new String((byte[]) payload, StandardCharsets.UTF_8);
      assertThat(payloadString)
          .isEqualTo(WebSocketErrorCode.FORBIDDEN_SUBSCRIPTION.getDefaultMessage());
    }
  }

  @Nested
  @DisplayName("기타 STOMP 명령")
  class OtherStompCommands {

    @Test
    @DisplayName("DISCONNECT 명령은 검증 없이 통과한다")
    void shouldPassDisconnectCommandWithoutValidation() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();
      assertThat(result).isEqualTo(message);

      verifyNoInteractions(jwtTokenProvider, userDetailsService);
    }
  }

  /**
   * 테스트용 Authentication 객체 생성
   *
   * @return Authentication 객체
   */
  private Authentication createAuthentication() {
    return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
        userDetails, null, userDetails.getAuthorities());
  }
}
