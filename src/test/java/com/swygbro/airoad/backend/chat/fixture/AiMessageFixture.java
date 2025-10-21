package com.swygbro.airoad.backend.chat.fixture;

import java.time.LocalDateTime;

import org.springframework.ai.chat.messages.MessageType;
import org.springframework.test.util.ReflectionTestUtils;

import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.chat.domain.entity.AiMessage;

/**
 * AiMessage 엔티티 테스트 픽스처
 *
 * <p>테스트에서 AiMessage 객체를 쉽게 생성하기 위한 유틸리티 클래스입니다.
 */
public class AiMessageFixture {

  /**
   * AiMessage 객체 생성
   *
   * @param id 메시지 ID
   * @param messageType 메시지 타입 (USER, ASSISTANT 등)
   * @param content 메시지 내용
   * @param conversation 연관된 대화 세션
   * @return AiMessage 객체
   */
  public static AiMessage createMessage(
      Long id, MessageType messageType, String content, AiConversation conversation) {
    AiMessage message =
        AiMessage.builder()
            .messageType(messageType)
            .content(content)
            .conversation(conversation)
            .build();
    ReflectionTestUtils.setField(message, "id", id);
    ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.now());
    return message;
  }

  /**
   * 사용자 메시지 생성
   *
   * @param id 메시지 ID
   * @param content 메시지 내용
   * @param conversation 연관된 대화 세션
   * @return AiMessage 객체
   */
  public static AiMessage createUserMessage(Long id, String content, AiConversation conversation) {
    return createMessage(id, MessageType.USER, content, conversation);
  }

  /**
   * AI 응답 메시지 생성
   *
   * @param id 메시지 ID
   * @param content 메시지 내용
   * @param conversation 연관된 대화 세션
   * @return AiMessage 객체
   */
  public static AiMessage createAssistantMessage(
      Long id, String content, AiConversation conversation) {
    return createMessage(id, MessageType.ASSISTANT, content, conversation);
  }
}
