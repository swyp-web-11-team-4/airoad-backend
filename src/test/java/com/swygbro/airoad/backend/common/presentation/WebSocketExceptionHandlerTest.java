package com.swygbro.airoad.backend.common.presentation;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.chat.presentation.message.WebSocketExceptionHandler;
import com.swygbro.airoad.backend.common.domain.dto.ErrorResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.common.exception.CommonErrorCode;
import com.swygbro.airoad.backend.common.exception.WebSocketErrorCode;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * WebSocketExceptionHandler 테스트
 *
 * <p>WebSocket 전역 예외 처리 핸들러의 동작을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("WebSocketExceptionHandler")
class WebSocketExceptionHandlerTest {

  @Mock private SimpMessagingTemplate messagingTemplate;

  @InjectMocks private WebSocketExceptionHandler exceptionHandler;

  @Mock private SimpMessageHeaderAccessor headerAccessor;

  private UserDetails userDetails;
  private static final String USER_EMAIL = "user@example.com";

  @BeforeEach
  void setUp() {
    userDetails =
        new User(USER_EMAIL, "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }

  @Nested
  @DisplayName("extractChatRoomId 메서드는")
  class ExtractChatRoomId {

    @Test
    @DisplayName("SEND 경로에서 chatRoomId를 추출한다")
    void shouldExtractChatRoomIdFromSendDestination() {
      // given
      given(headerAccessor.getDestination()).willReturn("/pub/chat/123/message");

      // when
      Long chatRoomId = invokExtractChatRoomId(headerAccessor);

      // then
      assertThat(chatRoomId).isEqualTo(123L);
    }

    @Test
    @DisplayName("SUBSCRIBE 경로에서 chatRoomId를 추출한다")
    void shouldExtractChatRoomIdFromSubscribeDestination() {
      // given
      given(headerAccessor.getDestination()).willReturn("/user/sub/chat/456");

      // when
      Long chatRoomId = invokExtractChatRoomId(headerAccessor);

      // then
      assertThat(chatRoomId).isEqualTo(456L);
    }

    @Test
    @DisplayName("잘못된 경로 패턴에서는 null을 반환한다")
    void shouldReturnNullForInvalidDestinationPattern() {
      // given
      given(headerAccessor.getDestination()).willReturn("/topic/public");

      // when
      Long chatRoomId = invokExtractChatRoomId(headerAccessor);

      // then
      assertThat(chatRoomId).isNull();
    }

    @Test
    @DisplayName("destination이 null이면 null을 반환한다")
    void shouldReturnNullWhenDestinationIsNull() {
      // given
      given(headerAccessor.getDestination()).willReturn(null);

      // when
      Long chatRoomId = invokExtractChatRoomId(headerAccessor);

      // then
      assertThat(chatRoomId).isNull();
    }

    @Test
    @DisplayName("headerAccessor가 null이면 null을 반환한다")
    void shouldReturnNullWhenHeaderAccessorIsNull() {
      // when
      Long chatRoomId = invokExtractChatRoomId(null);

      // then
      assertThat(chatRoomId).isNull();
    }

    @Test
    @DisplayName("chatRoomId가 숫자가 아니면 null을 반환한다")
    void shouldReturnNullWhenChatRoomIdIsNotNumeric() {
      // given
      given(headerAccessor.getDestination()).willReturn("/pub/chat/abc/message");

      // when
      Long chatRoomId = invokExtractChatRoomId(headerAccessor);

      // then
      assertThat(chatRoomId).isNull();
    }
  }

  @Nested
  @DisplayName("handleBusinessException 메서드는")
  class HandleBusinessException {

    @Test
    @DisplayName("BusinessException 발생 시 에러 메시지를 사용자에게 전송한다")
    void shouldSendErrorMessageToUser() {
      // given
      BusinessException exception = new BusinessException(CommonErrorCode.INVALID_INPUT);
      given(headerAccessor.getDestination()).willReturn("/pub/chat/123/message");

      ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<ErrorResponse> errorResponseCaptor =
          ArgumentCaptor.forClass(ErrorResponse.class);

      Principal principal =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

      // when
      exceptionHandler.handleBusinessException(exception, principal, headerAccessor);

      // then
      verify(messagingTemplate)
          .convertAndSendToUser(
              userIdCaptor.capture(), destinationCaptor.capture(), errorResponseCaptor.capture());

      assertThat(userIdCaptor.getValue()).isEqualTo(USER_EMAIL);
      assertThat(destinationCaptor.getValue()).isEqualTo("/sub/errors/123");

      ErrorResponse errorResponse = errorResponseCaptor.getValue();
      assertThat(errorResponse.code()).isEqualTo(CommonErrorCode.INVALID_INPUT.getCode());
      assertThat(errorResponse.message())
          .isEqualTo(CommonErrorCode.INVALID_INPUT.getDefaultMessage());
    }

    @Test
    @DisplayName("chatRoomId가 없으면 기본 에러 채널로 전송한다")
    void shouldSendToDefaultErrorChannelWhenChatRoomIdIsNull() {
      // given
      BusinessException exception = new BusinessException(CommonErrorCode.INVALID_INPUT);
      given(headerAccessor.getDestination()).willReturn("/topic/public");

      ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);

      Principal principal =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

      // when
      exceptionHandler.handleBusinessException(exception, principal, headerAccessor);

      // then
      verify(messagingTemplate)
          .convertAndSendToUser(
              eq(USER_EMAIL), destinationCaptor.capture(), any(ErrorResponse.class));

      assertThat(destinationCaptor.getValue()).isEqualTo("/sub/errors/unknown");
    }

    @Test
    @DisplayName("principal이 null이면 unknown 사용자로 전송한다")
    void shouldSendToUnknownUserWhenPrincipalIsNull() {
      // given
      BusinessException exception = new BusinessException(CommonErrorCode.INVALID_INPUT);
      given(headerAccessor.getDestination()).willReturn("/pub/chat/123/message");

      ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);

      // when
      exceptionHandler.handleBusinessException(exception, null, headerAccessor);

      // then
      verify(messagingTemplate)
          .convertAndSendToUser(userIdCaptor.capture(), anyString(), any(ErrorResponse.class));

      assertThat(userIdCaptor.getValue()).isEqualTo("unknown");
    }
  }

  @Nested
  @DisplayName("handleException 메서드는")
  class HandleException {

    @Test
    @DisplayName("일반 예외 발생 시 기본 에러 메시지를 사용자에게 전송한다")
    void shouldSendDefaultErrorMessageToUser() {
      // given
      Exception exception = new RuntimeException("Unexpected error");
      given(headerAccessor.getDestination()).willReturn("/pub/chat/456/message");

      ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<ErrorResponse> errorResponseCaptor =
          ArgumentCaptor.forClass(ErrorResponse.class);

      Principal principal =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

      // when
      exceptionHandler.handleException(exception, principal, headerAccessor);

      // then
      verify(messagingTemplate)
          .convertAndSendToUser(
              userIdCaptor.capture(), destinationCaptor.capture(), errorResponseCaptor.capture());

      assertThat(userIdCaptor.getValue()).isEqualTo(USER_EMAIL);
      assertThat(destinationCaptor.getValue()).isEqualTo("/sub/errors/456");

      ErrorResponse errorResponse = errorResponseCaptor.getValue();
      assertThat(errorResponse.code()).isEqualTo(WebSocketErrorCode.INTERNAL_ERROR.getCode());
      assertThat(errorResponse.message())
          .isEqualTo(WebSocketErrorCode.INTERNAL_ERROR.getDefaultMessage());
    }

    @Test
    @DisplayName("SUBSCRIBE 경로에서 예외 발생 시 올바른 에러 채널로 전송한다")
    void shouldSendToCorrectErrorChannelForSubscribeDestination() {
      // given
      Exception exception = new RuntimeException("Subscription error");
      given(headerAccessor.getDestination()).willReturn("/user/sub/chat/789");

      ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);

      Principal principal =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

      // when
      exceptionHandler.handleException(exception, principal, headerAccessor);

      // then
      verify(messagingTemplate)
          .convertAndSendToUser(
              eq(USER_EMAIL), destinationCaptor.capture(), any(ErrorResponse.class));

      assertThat(destinationCaptor.getValue()).isEqualTo("/sub/errors/789");
    }
  }

  /**
   * extractChatRoomId 메서드는 private이므로 reflection을 통해 테스트합니다.
   *
   * @param headerAccessor STOMP 메시지 헤더 접근자
   * @return chatRoomId
   */
  private Long invokExtractChatRoomId(SimpMessageHeaderAccessor headerAccessor) {
    try {
      var method =
          WebSocketExceptionHandler.class.getDeclaredMethod(
              "extractChatRoomId", SimpMessageHeaderAccessor.class);
      method.setAccessible(true);
      return (Long) method.invoke(exceptionHandler, headerAccessor);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke extractChatRoomId", e);
    }
  }
}
