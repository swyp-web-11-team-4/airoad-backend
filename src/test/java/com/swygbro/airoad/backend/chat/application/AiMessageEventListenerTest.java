package com.swygbro.airoad.backend.chat.application;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageResponse;
import com.swygbro.airoad.backend.chat.domain.dto.MessageContentType;
import com.swygbro.airoad.backend.chat.domain.event.AiMessageSavedEvent;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.*;

/**
 * AiMessageEventListener 테스트
 *
 * <p>AI 메시지 이벤트 처리 및 WebSocket 전송 로직을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("AiMessageEventListener 클래스")
class AiMessageEventListenerTest {

  @Mock private SimpMessagingTemplate messagingTemplate;

  @InjectMocks private AiMessageEventListener eventListener;

  @Nested
  @DisplayName("handleAiMessageSaved 메서드는")
  class HandleAiMessageSaved {

    @Test
    @DisplayName("AI 메시지 저장 이벤트를 받아 WebSocket으로 메시지를 전송한다")
    void shouldSendMessageViaWebSocket() {
      // given
      Long chatRoomId = 1L;
      String userId = "user123";
      String expectedDestination = "/sub/chat/" + chatRoomId;
      ChatMessageResponse response =
          new ChatMessageResponse(
              1L,
              MessageType.ASSISTANT,
              "서울 3박 4일 여행 일정을 생성했습니다.",
              MessageContentType.TEXT,
              null,
              LocalDateTime.now());
      AiMessageSavedEvent event = new AiMessageSavedEvent(chatRoomId, userId, response);

      // when
      eventListener.handleAiMessageSaved(event);

      // then
      then(messagingTemplate).should().convertAndSendToUser(userId, expectedDestination, response);
    }

    @Test
    @DisplayName("WebSocket 전송 실패 시 예외를 발생시키지 않고 로그만 남긴다")
    void shouldNotThrowExceptionWhenWebSocketFails() {
      // given
      Long chatRoomId = 2L;
      String userId = "user456";
      ChatMessageResponse response =
          new ChatMessageResponse(
              2L,
              MessageType.ASSISTANT,
              "부산 여행 일정을 생성했습니다.",
              MessageContentType.TEXT,
              null,
              LocalDateTime.now());
      AiMessageSavedEvent event = new AiMessageSavedEvent(chatRoomId, userId, response);

      String expectedDestination = "/sub/chat/" + chatRoomId;
      willThrow(new RuntimeException("WebSocket 연결 실패"))
          .given(messagingTemplate)
          .convertAndSendToUser(userId, expectedDestination, response);

      // when & then
      assertThatCode(() -> eventListener.handleAiMessageSaved(event)).doesNotThrowAnyException();

      then(messagingTemplate).should().convertAndSendToUser(userId, expectedDestination, response);
    }

    @Test
    @DisplayName("올바른 사용자 구독 경로로 메시지를 전송한다")
    void shouldSendToCorrectUserSubscriptionPath() {
      // given
      Long chatRoomId = 3L;
      String userId = "user789";
      ChatMessageResponse response =
          new ChatMessageResponse(
              3L,
              MessageType.ASSISTANT,
              "제주도 여행 일정입니다.",
              MessageContentType.TEXT,
              null,
              LocalDateTime.now());
      AiMessageSavedEvent event = new AiMessageSavedEvent(chatRoomId, userId, response);
      // 실제 WebSocket 경로는 /user/{userId}/sub/chat/{chatRoomId} 형태가 됨
      String expectedDestination = "/sub/chat/" + chatRoomId;

      // when
      eventListener.handleAiMessageSaved(event);

      // then
      then(messagingTemplate)
          .should()
          .convertAndSendToUser(eq(userId), eq(expectedDestination), eq(response));
    }

    @Test
    @DisplayName("다양한 메시지 타입에 대해 정상적으로 전송한다")
    void shouldHandleDifferentMessageTypes() {
      // given
      Long chatRoomId = 4L;
      String userId = "user999";
      ChatMessageResponse imageResponse =
          new ChatMessageResponse(
              4L,
              MessageType.ASSISTANT,
              "여행지 사진입니다.",
              MessageContentType.IMAGE,
              "https://example.com/image.jpg",
              LocalDateTime.now());
      AiMessageSavedEvent event = new AiMessageSavedEvent(chatRoomId, userId, imageResponse);

      // when
      eventListener.handleAiMessageSaved(event);

      // then
      String expectedDestination = "/sub/chat/" + chatRoomId;
      then(messagingTemplate)
          .should()
          .convertAndSendToUser(userId, expectedDestination, imageResponse);
    }
  }

  @Nested
  @DisplayName("이벤트 발행 및 수신 테스트")
  class EventPublishingAndListening {

    @Mock private ApplicationEventPublisher eventPublisher;

    @Captor private ArgumentCaptor<AiMessageSavedEvent> eventCaptor;

    @Test
    @DisplayName("이벤트 리스너가 발행된 이벤트를 정상적으로 수신한다")
    void shouldReceivePublishedEvent() {
      // given
      Long chatRoomId = 6L;
      String userId = "user222";
      ChatMessageResponse response =
          new ChatMessageResponse(
              6L,
              MessageType.ASSISTANT,
              "대전 여행 일정입니다.",
              MessageContentType.TEXT,
              null,
              LocalDateTime.now());
      AiMessageSavedEvent event = new AiMessageSavedEvent(chatRoomId, userId, response);

      // when - 이벤트 발행을 시뮬레이션하고 리스너 직접 호출
      eventListener.handleAiMessageSaved(event);

      // then - 리스너가 WebSocket 전송을 수행했는지 확인
      String expectedDestination = "/sub/chat/" + chatRoomId;
      then(messagingTemplate).should().convertAndSendToUser(userId, expectedDestination, response);
    }

    @Test
    @DisplayName("여러 이벤트가 발행되면 각각 독립적으로 처리된다")
    void shouldHandleMultipleEventsIndependently() {
      // given
      ChatMessageResponse response1 =
          new ChatMessageResponse(
              7L,
              MessageType.ASSISTANT,
              "첫 번째 응답",
              MessageContentType.TEXT,
              null,
              LocalDateTime.now());
      ChatMessageResponse response2 =
          new ChatMessageResponse(
              8L,
              MessageType.ASSISTANT,
              "두 번째 응답",
              MessageContentType.TEXT,
              null,
              LocalDateTime.now());

      AiMessageSavedEvent event1 = new AiMessageSavedEvent(1L, "user1", response1);
      AiMessageSavedEvent event2 = new AiMessageSavedEvent(2L, "user2", response2);

      // when
      eventListener.handleAiMessageSaved(event1);
      eventListener.handleAiMessageSaved(event2);

      // then
      then(messagingTemplate)
          .should(times(1))
          .convertAndSendToUser("user1", "/sub/chat/1", response1);
      then(messagingTemplate)
          .should(times(1))
          .convertAndSendToUser("user2", "/sub/chat/2", response2);
    }
  }
}
