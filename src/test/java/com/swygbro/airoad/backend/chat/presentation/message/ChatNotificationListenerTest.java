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

import com.swygbro.airoad.backend.ai.domain.event.AiMessageGeneratedEvent;
import com.swygbro.airoad.backend.chat.domain.dto.response.ChatStreamDto;
import com.swygbro.airoad.backend.common.domain.dto.ErrorResponse;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ChatNotificationListenerTest {

  @Mock private SimpMessagingTemplate messagingTemplate;

  @InjectMocks private ChatNotificationListener chatNotificationListener;

  @Nested
  @DisplayName("AI 메시지 생성 이벤트를 수신할 때")
  class HandleAiMessageGeneratedTests {

    @Test
    @DisplayName("WebSocket으로 AI 응답을 사용자에게 전송한다")
    void WebSocket으로_AI_응답을_사용자에게_전송() {
      // given
      AiMessageGeneratedEvent event =
          AiMessageGeneratedEvent.builder()
              .chatRoomId(1L)
              .username("test@example.com")
              .aiMessage("제주도 3박 4일 여행 계획을 추천드립니다.")
              .build();

      // when
      chatNotificationListener.handleAiMessageGenerated(event);

      // then
      verify(messagingTemplate)
          .convertAndSendToUser(
              eq("test@example.com"), eq("/sub/chat/1"), any(ChatStreamDto.class));
    }

    @Test
    @DisplayName("WebSocket 전송 실패 시 에러 메시지를 전송한다")
    void WebSocket_전송_실패_시_에러_메시지_전송() {
      // given
      AiMessageGeneratedEvent event =
          AiMessageGeneratedEvent.builder()
              .chatRoomId(1L)
              .username("test@example.com")
              .aiMessage("응답 메시지")
              .build();

      willThrow(new RuntimeException("전송 실패"))
          .given(messagingTemplate)
          .convertAndSendToUser(anyString(), anyString(), any(ChatStreamDto.class));

      // when
      chatNotificationListener.handleAiMessageGenerated(event);

      // then
      verify(messagingTemplate)
          .convertAndSendToUser(
              eq("test@example.com"), eq("/sub/errors/1"), any(ErrorResponse.class));
    }

    @Test
    @DisplayName("chatRoomId가 null이면 에러를 unknown 채널로 전송한다")
    void chatRoomId가_null이면_에러를_unknown_채널로_전송() {
      // given
      AiMessageGeneratedEvent event =
          AiMessageGeneratedEvent.builder()
              .chatRoomId(null)
              .username("test@example.com")
              .aiMessage("응답 메시지")
              .build();

      willThrow(new RuntimeException("전송 실패"))
          .given(messagingTemplate)
          .convertAndSendToUser(anyString(), anyString(), any(ChatStreamDto.class));

      // when
      chatNotificationListener.handleAiMessageGenerated(event);

      // then
      verify(messagingTemplate)
          .convertAndSendToUser(
              eq("test@example.com"), eq("/sub/errors/unknown"), any(ErrorResponse.class));
    }
  }
}
