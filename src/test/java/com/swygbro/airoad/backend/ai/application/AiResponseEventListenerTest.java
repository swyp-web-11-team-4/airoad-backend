package com.swygbro.airoad.backend.ai.application;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.ai.domain.dto.AiResponseContentType;
import com.swygbro.airoad.backend.ai.domain.event.AiResponseReceivedEvent;
import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.chat.domain.entity.AiMessage;
import com.swygbro.airoad.backend.chat.infrastructure.AiConversationRepository;
import com.swygbro.airoad.backend.chat.infrastructure.AiMessageRepository;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * AiResponseEventListener 테스트
 *
 * <p>AI 응답 수신 이벤트 처리, WebSocket 전송, DB 저장 로직을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("AiResponseEventListener 클래스")
class AiResponseEventListenerTest {

  @Mock private SimpMessagingTemplate messagingTemplate;
  @Mock private AiMessageRepository aiMessageRepository;
  @Mock private AiConversationRepository aiConversationRepository;

  @InjectMocks private AiResponseEventListener eventListener;

  @Nested
  @DisplayName("handleAiResponseReceived 메서드는")
  class HandleAiResponseReceived {

    @Test
    @DisplayName("CHAT 타입 응답을 채팅 채널로 전송한다")
    void shouldSendChatResponseToCorrectChannel() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 10L;
      String userId = "user123@example.com";
      String content = "서울 3박 4일 여행 일정을 생성했습니다.";
      String expectedDestination = "/sub/chat/" + chatRoomId;
      AiResponseReceivedEvent event =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, content, AiResponseContentType.CHAT, false);

      // when
      eventListener.handleAiResponseReceived(event);

      // then
      then(messagingTemplate).should().convertAndSendToUser(userId, expectedDestination, content);
    }

    @Test
    @DisplayName("SCHEDULE 타입 응답을 일정 채널로 전송한다")
    void shouldSendScheduleResponseToCorrectChannel() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 20L;
      String userId = "user123@example.com";
      String content = "{\"day\": 1, \"activities\": [...]}";
      String expectedDestination = "/sub/schedule/" + tripPlanId;
      AiResponseReceivedEvent event =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, content, AiResponseContentType.SCHEDULE, false);

      // when
      eventListener.handleAiResponseReceived(event);

      // then
      then(messagingTemplate).should().convertAndSendToUser(userId, expectedDestination, content);
    }

    @Test
    @DisplayName("WebSocket 전송 실패 시에도 완료된 메시지는 DB에 저장한다")
    void shouldSaveToDatabaseEvenWhenWebSocketFails() {
      // given
      Long chatRoomId = 2L;
      Long tripPlanId = 20L;
      String userId = "user456@example.com";
      String content = "부산 여행 일정입니다.";
      AiResponseReceivedEvent event =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, content, AiResponseContentType.CHAT, true);

      String expectedDestination = "/sub/chat/" + chatRoomId;
      willThrow(new RuntimeException("WebSocket 연결 실패"))
          .given(messagingTemplate)
          .convertAndSendToUser(userId, expectedDestination, content);

      AiConversation mockConversation = mock(AiConversation.class);
      given(aiConversationRepository.findById(chatRoomId))
          .willReturn(Optional.of(mockConversation));

      // when & then
      assertThatCode(() -> eventListener.handleAiResponseReceived(event))
          .doesNotThrowAnyException();

      then(messagingTemplate).should().convertAndSendToUser(userId, expectedDestination, content);
      then(aiConversationRepository).should().findById(chatRoomId);
      then(aiMessageRepository).should().save(any(AiMessage.class));
    }

    @Test
    @DisplayName("스트리밍 청크를 순차적으로 전송한다")
    void shouldSendStreamingChunksSequentially() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 10L;
      String userId = "user123@example.com";
      String expectedDestination = "/sub/chat/" + chatRoomId;

      AiResponseReceivedEvent chunk1 =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, "서울", AiResponseContentType.CHAT, false);
      AiResponseReceivedEvent chunk2 =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, " 3박", AiResponseContentType.CHAT, false);
      AiResponseReceivedEvent chunk3 =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, " 4일 여행", AiResponseContentType.CHAT, false);
      AiResponseReceivedEvent complete =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, " 일정입니다.", AiResponseContentType.CHAT, true);

      // when
      eventListener.handleAiResponseReceived(chunk1);
      eventListener.handleAiResponseReceived(chunk2);
      eventListener.handleAiResponseReceived(chunk3);
      eventListener.handleAiResponseReceived(complete);

      // then
      then(messagingTemplate)
          .should(times(1))
          .convertAndSendToUser(userId, expectedDestination, "서울");
      then(messagingTemplate)
          .should(times(1))
          .convertAndSendToUser(userId, expectedDestination, " 3박");
      then(messagingTemplate)
          .should(times(1))
          .convertAndSendToUser(userId, expectedDestination, " 4일 여행");
      then(messagingTemplate)
          .should(times(1))
          .convertAndSendToUser(userId, expectedDestination, " 일정입니다.");
    }

    @Test
    @DisplayName("여러 사용자의 응답을 독립적으로 처리한다")
    void shouldHandleMultipleUsersIndependently() {
      // given
      AiResponseReceivedEvent event1 =
          new AiResponseReceivedEvent(
              1L, 10L, "user1@example.com", "첫 번째 응답", AiResponseContentType.CHAT, false);
      AiResponseReceivedEvent event2 =
          new AiResponseReceivedEvent(
              2L, 20L, "user2@example.com", "두 번째 응답", AiResponseContentType.CHAT, false);

      // when
      eventListener.handleAiResponseReceived(event1);
      eventListener.handleAiResponseReceived(event2);

      // then
      then(messagingTemplate)
          .should(times(1))
          .convertAndSendToUser("user1@example.com", "/sub/chat/1", "첫 번째 응답");
      then(messagingTemplate)
          .should(times(1))
          .convertAndSendToUser("user2@example.com", "/sub/chat/2", "두 번째 응답");
    }

    @Test
    @DisplayName("완료된 CHAT 메시지를 DB에 저장한다")
    void shouldSaveChatMessageToDatabase() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 10L;
      String userId = "user123@example.com";
      String content = "서울 3박 4일 여행 일정입니다.";
      AiResponseReceivedEvent event =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, content, AiResponseContentType.CHAT, true);

      AiConversation mockConversation = mock(AiConversation.class);
      given(aiConversationRepository.findById(chatRoomId))
          .willReturn(Optional.of(mockConversation));

      // when
      eventListener.handleAiResponseReceived(event);

      // then
      then(messagingTemplate)
          .should()
          .convertAndSendToUser(userId, "/sub/chat/" + chatRoomId, content);
      then(aiConversationRepository).should().findById(chatRoomId);
      then(aiMessageRepository).should().save(any(AiMessage.class));
    }

    @Test
    @DisplayName("미완료 CHAT 메시지는 DB에 저장하지 않는다")
    void shouldNotSaveIncompleteChatMessage() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 10L;
      String userId = "user123@example.com";
      String content = "서울";
      AiResponseReceivedEvent event =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, content, AiResponseContentType.CHAT, false);

      // when
      eventListener.handleAiResponseReceived(event);

      // then
      then(messagingTemplate)
          .should()
          .convertAndSendToUser(userId, "/sub/chat/" + chatRoomId, content);
      then(aiConversationRepository).should(never()).findById(any());
      then(aiMessageRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("완료된 SCHEDULE 메시지는 WebSocket 전송하지만 DB 저장은 TODO 상태다")
    void shouldSendScheduleButNotSaveYet() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 10L;
      String userId = "user123@example.com";
      String content = "{\"day\": 1, \"activities\": [...]}";
      AiResponseReceivedEvent event =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, content, AiResponseContentType.SCHEDULE, true);

      // when
      eventListener.handleAiResponseReceived(event);

      // then
      then(messagingTemplate)
          .should()
          .convertAndSendToUser(userId, "/sub/schedule/" + tripPlanId, content);
      then(aiMessageRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("DB 저장 실패 시에도 예외를 발생시키지 않는다")
    void shouldNotThrowExceptionWhenDatabaseFails() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 10L;
      String userId = "user123@example.com";
      String content = "서울 여행 일정입니다.";
      AiResponseReceivedEvent event =
          new AiResponseReceivedEvent(
              chatRoomId, tripPlanId, userId, content, AiResponseContentType.CHAT, true);

      given(aiConversationRepository.findById(chatRoomId))
          .willThrow(new RuntimeException("DB 연결 실패"));

      // when & then
      assertThatCode(() -> eventListener.handleAiResponseReceived(event))
          .doesNotThrowAnyException();

      then(messagingTemplate)
          .should()
          .convertAndSendToUser(userId, "/sub/chat/" + chatRoomId, content);
    }
  }
}
