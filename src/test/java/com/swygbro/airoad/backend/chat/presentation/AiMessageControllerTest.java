package com.swygbro.airoad.backend.chat.presentation;

import java.security.Principal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.chat.application.AiMessageService;
import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageRequest;
import com.swygbro.airoad.backend.chat.domain.dto.MessageContentType;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AiMessageControllerTest {

  @InjectMocks private AiMessageController aiMessageController;

  @Mock private AiMessageService aiMessageService;

  @Mock private Principal principal;

  @Nested
  @DisplayName("sendMessage 메서드는")
  class SendMessage {

    @Test
    @DisplayName("유효한 메시지와 인증된 사용자로 메시지를 처리한다")
    void shouldProcessMessageWithValidRequestAndAuthenticatedUser() {
      // given
      Long chatRoomId = 1L;
      String userId = "user123";
      ChatMessageRequest request =
          new ChatMessageRequest("서울 3박 4일 여행 계획을 짜주세요", MessageContentType.TEXT);

      given(principal.getName()).willReturn(userId);
      willDoNothing().given(aiMessageService).processAndSendMessage(chatRoomId, userId, request);

      // when
      aiMessageController.sendMessage(chatRoomId, request, principal);

      // then
      verify(principal).getName();
      verify(aiMessageService).processAndSendMessage(chatRoomId, userId, request);
    }

    @Test
    @DisplayName("Principal이 null이면 IllegalStateException을 던진다")
    void shouldThrowExceptionWhenPrincipalIsNull() {
      // given
      Long chatRoomId = 1L;
      ChatMessageRequest request =
          new ChatMessageRequest("서울 3박 4일 여행 계획을 짜주세요", MessageContentType.TEXT);

      // when & then
      assertThatThrownBy(() -> aiMessageController.sendMessage(chatRoomId, request, null))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("인증되지 않은 사용자는 채팅을 사용할 수 없습니다.");

      verifyNoInteractions(aiMessageService);
    }

    @Test
    @DisplayName("빈 메시지 내용으로도 서비스 레이어로 전달한다")
    void shouldPassEmptyContentToServiceLayer() {
      // given
      Long chatRoomId = 3L;
      String userId = "user789";
      // Validation은 Spring이 처리하므로, Controller 단위 테스트에서는 검증하지 않음
      ChatMessageRequest request = new ChatMessageRequest("", MessageContentType.TEXT);

      given(principal.getName()).willReturn(userId);
      willDoNothing().given(aiMessageService).processAndSendMessage(chatRoomId, userId, request);

      // when
      aiMessageController.sendMessage(chatRoomId, request, principal);

      // then
      verify(aiMessageService).processAndSendMessage(chatRoomId, userId, request);
    }
  }
}
