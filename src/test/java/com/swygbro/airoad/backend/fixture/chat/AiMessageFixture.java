package com.swygbro.airoad.backend.fixture.chat;

import org.springframework.ai.chat.messages.MessageType;

import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.chat.domain.entity.AiMessage;

public class AiMessageFixture {

  public static AiMessage create() {
    return AiMessage.builder()
        .conversation(AiConversationFixture.create())
        .messageType(MessageType.USER)
        .content("서울 3박 4일 여행 일정을 추천해주세요.")
        .build();
  }

  public static AiMessage createUserMessage() {
    return AiMessage.builder()
        .conversation(AiConversationFixture.create())
        .messageType(MessageType.USER)
        .content("제주도 2박 3일 여행 일정을 추천해주세요.")
        .build();
  }

  public static AiMessage createAssistantMessage() {
    return AiMessage.builder()
        .conversation(AiConversationFixture.create())
        .messageType(MessageType.ASSISTANT)
        .content("제주도 2박 3일 여행 일정을 추천해드리겠습니다. 어떤 테마의 여행을 원하시나요?")
        .build();
  }

  public static AiMessage createSystemMessage() {
    return AiMessage.builder()
        .conversation(AiConversationFixture.create())
        .messageType(MessageType.SYSTEM)
        .content("여행 일정 추천 AI입니다. 무엇을 도와드릴까요?")
        .build();
  }

  public static AiMessage createWithConversation(AiConversation conversation) {
    return AiMessage.builder()
        .conversation(conversation)
        .messageType(MessageType.USER)
        .content("서울 3박 4일 여행 일정을 추천해주세요.")
        .build();
  }

  public static AiMessage createWithConversationAndContent(
      AiConversation conversation, String content) {
    return AiMessage.builder()
        .conversation(conversation)
        .messageType(MessageType.USER)
        .content(content)
        .build();
  }

  public static AiMessage.AiMessageBuilder builder() {
    return AiMessage.builder()
        .conversation(AiConversationFixture.create())
        .messageType(MessageType.USER)
        .content("테스트 메시지");
  }
}
