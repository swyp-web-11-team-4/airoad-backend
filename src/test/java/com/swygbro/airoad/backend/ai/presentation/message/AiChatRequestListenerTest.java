package com.swygbro.airoad.backend.ai.presentation.message;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.chat.domain.event.AiChatRequestedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * AiChatRequestListener의 테스트 클래스입니다.
 *
 * <p>AI 채팅 요청 이벤트를 수신하여 AI 서비스를 호출하고 스트리밍 응답을 처리하는 기능을 검증합니다.
 */
@Disabled("TODO: 실제 AI 서비스 연동 후 테스트 활성화")
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("AiChatRequestListener 클래스")
class AiChatRequestListenerTest {

  @InjectMocks private AiChatRequestListener listener;

  // 테스트 데이터 상수
  private static final Long TEST_CHAT_ROOM_ID = 1L;
  private static final Long TEST_TRIP_PLAN_ID = 100L;
  private static final String TEST_USER_ID = "test@example.com";
  private static final String TEST_USER_MESSAGE = "안녕하세요, AI!";

  /**
   * 테스트용 AiRequestEvent를 생성하는 헬퍼 메서드입니다.
   *
   * @return 테스트용 AiRequestEvent
   */
  private AiChatRequestedEvent createTestAiRequestEvent() {
    return new AiChatRequestedEvent(
        TEST_CHAT_ROOM_ID, TEST_TRIP_PLAN_ID, TEST_USER_ID, TEST_USER_MESSAGE);
  }

  @Nested
  @DisplayName("handleAiChatRequest 메서드는")
  class HandleAiChatRequest {

    @Test
    @DisplayName("AI 채팅 요청 이벤트를 수신하고 예외 없이 처리한다")
    void shouldReceiveEventWithoutException() {
      // given - AI 채팅 요청 이벤트 준비
      AiChatRequestedEvent event = createTestAiRequestEvent();

      // when & then - 이벤트 처리 시 예외가 발생하지 않음
      assertThatCode(() -> listener.handleAiChatRequest(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("chatRoomId, tripPlanId, userId를 포함한 이벤트를 정상 처리한다")
    void shouldHandleEventWithAllIdentifiers() {
      // given - 모든 식별자를 포함한 이벤트 준비
      AiChatRequestedEvent event = createTestAiRequestEvent();

      // when & then - 정상적으로 처리됨
      assertThatCode(() -> listener.handleAiChatRequest(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("사용자 메시지가 포함된 요청을 정상 처리한다")
    void shouldHandleEventWithUserMessage() {
      // given - 사용자 메시지가 포함된 이벤트 준비
      AiChatRequestedEvent event = createTestAiRequestEvent();

      // when & then - 사용자 메시지가 포함된 이벤트도 정상 처리됨
      assertThatCode(() -> listener.handleAiChatRequest(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다양한 길이의 메시지를 처리한다")
    void shouldHandleMessagesOfVariousLengths() {
      // given - 짧은 메시지
      AiChatRequestedEvent shortMessageEvent =
          new AiChatRequestedEvent(TEST_CHAT_ROOM_ID, TEST_TRIP_PLAN_ID, TEST_USER_ID, "안녕");

      // when & then - 짧은 메시지 정상 처리
      assertThatCode(() -> listener.handleAiChatRequest(shortMessageEvent))
          .doesNotThrowAnyException();

      // given - 긴 메시지
      String longMessage = "안녕하세요, AI! 저는 제주도 여행을 계획하고 있는데요. ".repeat(10);
      AiChatRequestedEvent longMessageEvent =
          new AiChatRequestedEvent(TEST_CHAT_ROOM_ID, TEST_TRIP_PLAN_ID, TEST_USER_ID, longMessage);

      // when & then - 긴 메시지 정상 처리
      assertThatCode(() -> listener.handleAiChatRequest(longMessageEvent))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("여행 일정 관련 질문을 처리한다")
    void shouldHandleTripPlanningQuestions() {
      // given - 여행 일정 관련 질문
      AiChatRequestedEvent event =
          new AiChatRequestedEvent(
              TEST_CHAT_ROOM_ID, TEST_TRIP_PLAN_ID, TEST_USER_ID, "제주도에서 가볼만한 맛집 추천해줘");

      // when & then - 여행 일정 관련 질문 정상 처리
      assertThatCode(() -> listener.handleAiChatRequest(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("일정 수정 요청을 처리한다")
    void shouldHandleScheduleModificationRequests() {
      // given - 일정 수정 요청
      AiChatRequestedEvent event =
          new AiChatRequestedEvent(
              TEST_CHAT_ROOM_ID, TEST_TRIP_PLAN_ID, TEST_USER_ID, "2일차 일정에서 점심 식사 장소를 바꿔줘");

      // when & then - 일정 수정 요청 정상 처리
      assertThatCode(() -> listener.handleAiChatRequest(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("여러 사용자의 요청을 독립적으로 처리한다")
    void shouldHandleMultipleUsersIndependently() {
      // given - 첫 번째 사용자의 요청
      AiChatRequestedEvent event1 = new AiChatRequestedEvent(1L, 100L, "user1@example.com", "첫 번째 사용자 메시지");

      // when & then - 첫 번째 사용자 요청 정상 처리
      assertThatCode(() -> listener.handleAiChatRequest(event1)).doesNotThrowAnyException();

      // given - 두 번째 사용자의 요청
      AiChatRequestedEvent event2 = new AiChatRequestedEvent(2L, 200L, "user2@example.com", "두 번째 사용자 메시지");

      // when & then - 두 번째 사용자 요청 정상 처리
      assertThatCode(() -> listener.handleAiChatRequest(event2)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("동일 사용자의 연속된 요청을 처리한다")
    void shouldHandleConsecutiveRequestsFromSameUser() {
      // given - 첫 번째 메시지
      AiChatRequestedEvent event1 = createTestAiRequestEvent();

      // when & then - 첫 번째 메시지 정상 처리
      assertThatCode(() -> listener.handleAiChatRequest(event1)).doesNotThrowAnyException();

      // given - 두 번째 메시지
      AiChatRequestedEvent event2 =
          new AiChatRequestedEvent(TEST_CHAT_ROOM_ID, TEST_TRIP_PLAN_ID, TEST_USER_ID, "추가 질문입니다");

      // when & then - 두 번째 메시지 정상 처리
      assertThatCode(() -> listener.handleAiChatRequest(event2)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("비동기 처리")
  class AsyncProcessing {

    @Test
    @DisplayName("이벤트 처리는 @Async 어노테이션으로 비동기 실행된다")
    void shouldBeAnnotatedWithAsync() throws NoSuchMethodException {
      // given - handleAiChatRequest 메서드 확인

      // when - 메서드에 @Async 어노테이션이 있는지 확인
      boolean hasAsyncAnnotation =
          listener
              .getClass()
              .getMethod("handleAiChatRequest", AiChatRequestedEvent.class)
              .isAnnotationPresent(org.springframework.scheduling.annotation.Async.class);

      // then - @Async 어노테이션이 존재함
      assertThat(hasAsyncAnnotation).isTrue();
    }

    @Test
    @DisplayName("이벤트 처리는 @EventListener 어노테이션으로 이벤트를 수신한다")
    void shouldBeAnnotatedWithEventListener() throws NoSuchMethodException {
      // given - handleAiChatRequest 메서드 확인

      // when - 메서드에 @EventListener 어노테이션이 있는지 확인
      boolean hasEventListenerAnnotation =
          listener
              .getClass()
              .getMethod("handleAiChatRequest", AiChatRequestedEvent.class)
              .isAnnotationPresent(org.springframework.context.event.EventListener.class);

      // then - @EventListener 어노테이션이 존재함
      assertThat(hasEventListenerAnnotation).isTrue();
    }
  }
}
