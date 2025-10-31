package com.swygbro.airoad.backend.ai.agent.chat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.chat.domain.event.AiChatRequestedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ChatAgentTest {

  @Mock private ChatModel chatModel;

  @Mock private ChatMemory chatMemory;

  @Mock private ApplicationEventPublisher eventPublisher;

  private ChatAgent chatAgent;

  @BeforeEach
  void setUp() {
    chatAgent = new ChatAgent(chatModel, chatMemory, eventPublisher);
  }

  @Nested
  @DisplayName("에이전트 동작은")
  class Supports {

    @Test
    @DisplayName("식별자가 일치해야 동작한다")
    void shouldReturnTrueForChatAgent() {
      // when
      boolean result = chatAgent.supports("chatAgent");

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("알 수 없는 에이전트 식별자는 지원하지 않아야 한다")
    void shouldReturnFalseForUnknownAgent() {
      // when
      boolean result = chatAgent.supports("unknownAgent");

      // then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("에이전트를 호출하면")
  class Execute {

    @Test
    @DisplayName("다양한 사용자 메시지를 처리한다")
    void shouldHandleVariousUserMessages() {
      // given
      List<String> messages =
          List.of("서울 여행 일정 알려줘", "부산 맛집 추천해줘", "제주도 3박 4일 일정 짜줘", "일정 변경하고 싶어요", "다른 장소로 바꿔줘");

      // when & then
      for (String message : messages) {
        AiChatRequestedEvent testEvent =
            AiChatRequestedEvent.builder()
                .chatRoomId(1L)
                .tripPlanId(1L)
                .username("testuser@example.com")
                .userMessage(message)
                .build();

        assertThatCode(() -> chatAgent.execute(testEvent)).doesNotThrowAnyException();
      }
    }

    @Test
    @DisplayName("다양한 사용자명으로 요청을 처리한다")
    void shouldHandleVariousUsernames() {
      // given
      List<String> usernames =
          List.of("user1@example.com", "user2@example.com", "admin@example.com");

      // when & then
      for (String username : usernames) {
        AiChatRequestedEvent testEvent =
            AiChatRequestedEvent.builder()
                .chatRoomId(1L)
                .tripPlanId(1L)
                .username(username)
                .userMessage("여행 계획 알려줘")
                .build();

        assertThatCode(() -> chatAgent.execute(testEvent)).doesNotThrowAnyException();
      }
    }
  }
}
