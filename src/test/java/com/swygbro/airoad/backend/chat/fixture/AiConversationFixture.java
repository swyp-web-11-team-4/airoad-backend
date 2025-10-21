package com.swygbro.airoad.backend.chat.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;

/**
 * AiConversation 엔티티 테스트 픽스처
 *
 * <p>테스트에서 AiConversation 객체를 쉽게 생성하기 위한 유틸리티 클래스입니다.
 */
public class AiConversationFixture {

  /**
   * 기본 AiConversation 객체 생성
   *
   * @param id 대화 세션 ID
   * @return AiConversation 객체
   */
  public static AiConversation createConversation(Long id) {
    AiConversation conversation = AiConversation.builder().member(null).tripPlan(null).build();
    ReflectionTestUtils.setField(conversation, "id", id);
    return conversation;
  }

  /**
   * 기본 ID(1L)를 가진 AiConversation 객체 생성
   *
   * @return AiConversation 객체
   */
  public static AiConversation createConversation() {
    return AiConversation.builder().build();
  }
}
