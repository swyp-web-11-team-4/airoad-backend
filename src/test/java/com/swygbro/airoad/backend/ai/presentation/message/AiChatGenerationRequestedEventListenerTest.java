package com.swygbro.airoad.backend.ai.presentation.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.ai.application.AiUseCase;
import com.swygbro.airoad.backend.chat.domain.event.AiChatRequest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AiChatRequestListenerTest {

  @Mock private AiUseCase aiUseCase;

  @InjectMocks private AiChatRequestListener aiChatRequestListener;

  @Nested
  @DisplayName("AI 채팅 요청 이벤트를 수신할 때")
  class HandleAiChatRequestTests {

    @Test
    @DisplayName("이벤트를 chatAgent에게 그대로 전달한다")
    void 이벤트를_chatAgent에게_그대로_전달() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 100L;
      String username = "test@example.com";
      String userMessage = "여행 일정을 수정해주세요.";

      AiChatRequest event =
          AiChatRequest.builder()
              .chatRoomId(chatRoomId)
              .tripPlanId(tripPlanId)
              .username(username)
              .userMessage(userMessage)
              .build();

      // when
      aiChatRequestListener.handleAiChatRequest(event);

      // then
      ArgumentCaptor<AiChatRequest>
          requestCaptor =
              ArgumentCaptor.forClass(
                  AiChatRequest.class);
      verify(aiUseCase).agentCall(eq("chatAgent"), requestCaptor.capture());

      AiChatRequest capturedRequest =
          requestCaptor.getValue();
      assertThat(capturedRequest.chatRoomId()).isEqualTo(chatRoomId);
      assertThat(capturedRequest.tripPlanId()).isEqualTo(tripPlanId);
      assertThat(capturedRequest.username()).isEqualTo(username);
      assertThat(capturedRequest.userMessage()).isEqualTo(userMessage);
    }

    @Test
    @DisplayName("정확히 chatAgent를 호출한다")
    void 정확히_chatAgent를_호출() {
      // given
      AiChatRequest event =
          AiChatRequest.builder()
              .chatRoomId(2L)
              .tripPlanId(200L)
              .username("user@example.com")
              .userMessage("테스트 메시지")
              .build();

      // when
      aiChatRequestListener.handleAiChatRequest(event);

      // then
      verify(aiUseCase, times(1))
          .agentCall(
              eq("chatAgent"),
              any(com.swygbro.airoad.backend.ai.agent.chat.dto.request.AiChatRequest.class));
    }
  }
}
