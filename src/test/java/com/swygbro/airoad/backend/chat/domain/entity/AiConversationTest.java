package com.swygbro.airoad.backend.chat.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.swygbro.airoad.backend.chat.fixture.AiConversationFixture;

import static org.assertj.core.api.Assertions.*;

/**
 * AiConversation 엔티티 테스트
 *
 * <p>AiConversation 엔티티의 비즈니스 로직을 검증합니다.
 */
@DisplayName("AiConversation 엔티티")
class AiConversationTest {

  @Nested
  @DisplayName("isOwner 메서드는")
  class IsOwner {

    @Test
    @DisplayName("소유자의 이메일이 일치하면 true를 반환한다")
    void shouldReturnTrueWhenEmailMatches() {
      // given
      String ownerEmail = "owner@example.com";
      AiConversation conversation = AiConversationFixture.createConversation(1L, ownerEmail);

      // when
      boolean result = conversation.isOwner(ownerEmail);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("소유자의 이메일이 일치하지 않으면 false를 반환한다")
    void shouldReturnFalseWhenEmailDoesNotMatch() {
      // given
      String ownerEmail = "owner@example.com";
      String otherEmail = "other@example.com";
      AiConversation conversation = AiConversationFixture.createConversation(1L, ownerEmail);

      // when
      boolean result = conversation.isOwner(otherEmail);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("email이 null이면 IllegalArgumentException을 발생시킨다")
    void shouldThrowExceptionWhenEmailIsNull() {
      // given
      String ownerEmail = "owner@example.com";
      AiConversation conversation = AiConversationFixture.createConversation(1L, ownerEmail);

      // when & then
      assertThatThrownBy(() -> conversation.isOwner(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("email은 null일 수 없습니다");
    }
  }

  @Nested
  @DisplayName("getTripPlanId 메서드는")
  class GetTripPlanId {

    @Test
    @DisplayName("연관된 여행 계획의 ID를 반환한다")
    void shouldReturnTripPlanId() {
      // given
      String ownerEmail = "owner@example.com";
      AiConversation conversation = AiConversationFixture.createConversation(1L, ownerEmail);

      // when
      Long tripPlanId = conversation.getTripPlanId();

      // then
      assertThat(tripPlanId).isNotNull();
      assertThat(tripPlanId).isEqualTo(1L);
    }
  }
}
