package com.swygbro.airoad.backend.chat.presentation.web;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.chat.application.AiMessageService;
import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageRequest;
import com.swygbro.airoad.backend.chat.domain.dto.MessageContentType;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.common.exception.WebSocketErrorCode;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AiMessageControllerTest {

  @InjectMocks private AiMessageController aiMessageController;

  @Mock private AiMessageService aiMessageService;

  @Nested
  @DisplayName("sendMessage 메서드는")
  class SendMessage {

    @Test
    @DisplayName("유효한 메시지와 인증된 사용자로 메시지를 처리한다")
    void shouldProcessMessageWithValidRequestAndAuthenticatedUser() {
      // given
      Long chatRoomId = 1L;
      String username = "test@example.com";
      ChatMessageRequest request =
          new ChatMessageRequest("서울 3박 4일 여행 계획을 짜주세요", MessageContentType.TEXT);

      // StompHeaderAccessor 생성 및 인증 정보 설정
      UserDetails userDetails =
          User.builder()
              .username(username)
              .password("password")
              .authorities(Collections.emptyList())
              .build();
      Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null);

      // Message<ChatMessageRequest> 생성
      Message<ChatMessageRequest> message = new GenericMessage<>(request);
      StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);
      headerAccessor.setUser(authentication);

      willDoNothing().given(aiMessageService).processAndSendMessage(chatRoomId, username, request);

      // when
      aiMessageController.sendMessage(chatRoomId, message, headerAccessor);

      // then
      verify(aiMessageService).processAndSendMessage(chatRoomId, username, request);
    }

    @Test
    @DisplayName("Authentication이 null이면 BusinessException을 던진다")
    void shouldThrowExceptionWhenAuthenticationIsNull() {
      // given
      Long chatRoomId = 1L;
      ChatMessageRequest request =
          new ChatMessageRequest("서울 3박 4일 여행 계획을 짜주세요", MessageContentType.TEXT);

      Message<ChatMessageRequest> message = new GenericMessage<>(request);
      StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);
      // Authentication을 설정하지 않음 (null)

      // when & then
      assertThatThrownBy(() -> aiMessageController.sendMessage(chatRoomId, message, headerAccessor))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", WebSocketErrorCode.UNAUTHORIZED_CONNECTION);

      verifyNoInteractions(aiMessageService);
    }

    @Test
    @DisplayName("Principal이 UserDetails 타입이 아니면 BusinessException을 던진다")
    void shouldThrowExceptionWhenPrincipalIsNotUserDetails() {
      // given
      Long chatRoomId = 1L;
      ChatMessageRequest request =
          new ChatMessageRequest("서울 3박 4일 여행 계획을 짜주세요", MessageContentType.TEXT);

      Message<ChatMessageRequest> message = new GenericMessage<>(request);
      StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);
      // UserDetails가 아닌 다른 Principal 설정
      Authentication authentication =
          new UsernamePasswordAuthenticationToken("stringPrincipal", null);
      headerAccessor.setUser(authentication);

      // when & then
      assertThatThrownBy(() -> aiMessageController.sendMessage(chatRoomId, message, headerAccessor))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", WebSocketErrorCode.UNAUTHORIZED_CONNECTION);

      verifyNoInteractions(aiMessageService);
    }
  }
}
