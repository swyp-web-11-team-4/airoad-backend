package com.swygbro.airoad.backend.chat.application;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageRequest;
import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageResponse;
import com.swygbro.airoad.backend.chat.domain.dto.MessageContentType;
import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.chat.domain.entity.AiMessage;
import com.swygbro.airoad.backend.chat.domain.event.AiMessageSavedEvent;
import com.swygbro.airoad.backend.chat.exception.ChatErrorCode;
import com.swygbro.airoad.backend.chat.fixture.AiConversationFixture;
import com.swygbro.airoad.backend.chat.fixture.AiMessageFixture;
import com.swygbro.airoad.backend.chat.infrastructure.AiConversationRepository;
import com.swygbro.airoad.backend.chat.infrastructure.AiMessageRepository;
import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AiMessageServiceTest {

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private AiMessageRepository aiMessageRepository;

  @Mock private AiConversationRepository aiConversationRepository;

  @InjectMocks private AiMessageService aiMessageService;

  @Captor ArgumentCaptor<AiMessage> messageCaptor;

  @Captor ArgumentCaptor<AiMessageSavedEvent> eventCaptor;

  @Nested
  @DisplayName("processAndSendMessage 메서드는")
  class ProcessAndSendMessage {

    @Test
    @DisplayName("TEXT 메시지를 정상적으로 처리하고 AI 응답을 생성한다")
    void shouldProcessTextMessageSuccessfully() {
      // given
      Long chatRoomId = 1L;
      String userId = "user123";
      String messageContent = "서울 3박 4일 여행 계획을 짜주세요";
      ChatMessageRequest request = new ChatMessageRequest(messageContent, MessageContentType.TEXT);

      AiConversation conversation = AiConversationFixture.createConversation(chatRoomId);
      AiMessage userMessage = AiMessageFixture.createUserMessage(1L, messageContent, conversation);
      AiMessage aiMessage =
          AiMessageFixture.createAssistantMessage(
              2L, "[AI 응답] " + messageContent + "에 대한 답변입니다.", conversation);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));
      given(aiMessageRepository.save(any(AiMessage.class)))
          .willReturn(userMessage, aiMessage)
          .willAnswer(
              inv -> {
                throw new AssertionError("save called more than twice");
              });

      // when
      aiMessageService.processAndSendMessage(chatRoomId, userId, request);

      // then
      verify(aiConversationRepository).findById(chatRoomId);
      verify(aiMessageRepository, times(2)).save(messageCaptor.capture());
      var first = messageCaptor.getAllValues().get(0);
      var second = messageCaptor.getAllValues().get(1);

      // 1) 첫 저장: USER 텍스트
      assertThat(first.getConversation().getId()).isEqualTo(chatRoomId);
      assertThat(first.getMessageType()).isEqualTo(MessageType.USER);
      assertThat(first.getContent()).isEqualTo(messageContent);

      // 2) 두 번째 저장: ASSISTANT 응답
      assertThat(second.getConversation().getId()).isEqualTo(chatRoomId);
      assertThat(second.getMessageType()).isEqualTo(MessageType.ASSISTANT);
      assertThat(second.getContent()).contains(messageContent);

      // 3) 이벤트 발행 검증
      verify(eventPublisher).publishEvent(eventCaptor.capture());
      AiMessageSavedEvent publishedEvent = eventCaptor.getValue();
      assertThat(publishedEvent.chatRoomId()).isEqualTo(chatRoomId);
      assertThat(publishedEvent.userId()).isEqualTo(userId);
      assertThat(publishedEvent.response().id()).isEqualTo(aiMessage.getId());
      assertThat(publishedEvent.response().messageType()).isEqualTo(MessageType.ASSISTANT);
      assertThat(publishedEvent.response().content()).isEqualTo(aiMessage.getContent());

      verifyNoMoreInteractions(aiConversationRepository, aiMessageRepository, eventPublisher);
    }

    @Test
    @DisplayName("존재하지 않는 채팅방 ID로 요청 시 BusinessException을 발생시킨다")
    void shouldThrowExceptionWhenChatRoomNotFound() {
      // given
      Long chatRoomId = 999L;
      String userId = "user123";
      ChatMessageRequest request = new ChatMessageRequest("안녕하세요", MessageContentType.TEXT);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> aiMessageService.processAndSendMessage(chatRoomId, userId, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CONVERSATION_NOT_FOUND);

      verify(aiConversationRepository).findById(chatRoomId);
      verify(aiMessageRepository, never()).save(any());
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("TEXT 타입이 아닌 메시지는 BusinessException을 발생시킨다")
    void shouldThrowExceptionForNonTextMessage() {
      // given
      Long chatRoomId = 1L;
      String userId = "user123";
      ChatMessageRequest request =
          new ChatMessageRequest("image content", MessageContentType.IMAGE);

      AiConversation conversation = AiConversationFixture.createConversation(chatRoomId);
      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));

      // when & then
      assertThatThrownBy(() -> aiMessageService.processAndSendMessage(chatRoomId, userId, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.INVALID_MESSAGE_FORMAT);

      verify(aiConversationRepository).findById(chatRoomId);
      verify(aiMessageRepository, never()).save(any());
      verify(eventPublisher, never()).publishEvent(any());
    }
  }

  @Nested
  @DisplayName("getMessageHistory 메서드는")
  class GetMessageHistory {

    @Test
    @DisplayName("커서 없이 최신 메시지부터 조회한다")
    void shouldGetLatestMessagesWithoutCursor() {
      // given
      Long chatRoomId = 1L;
      int size = 50;
      AiConversation conversation = AiConversationFixture.createConversation(chatRoomId);

      List<AiMessage> messages =
          List.of(
              AiMessageFixture.createAssistantMessage(3L, "AI 응답 3", conversation),
              AiMessageFixture.createUserMessage(2L, "사용자 메시지 2", conversation),
              AiMessageFixture.createAssistantMessage(1L, "AI 응답 1", conversation));

      SliceImpl<AiMessage> messageSlice = new SliceImpl<>(messages, PageRequest.of(0, size), false);

      given(aiConversationRepository.existsById(chatRoomId)).willReturn(true);
      given(
              aiMessageRepository.findMessageHistoryByCursor(
                  eq(chatRoomId), isNull(), any(Pageable.class)))
          .willReturn(messageSlice);

      // when
      CursorPageResponse<ChatMessageResponse> response =
          aiMessageService.getMessageHistory(chatRoomId, null, size);

      // then
      assertThat(response.getContent()).hasSize(3);
      assertThat(response.getNextCursor()).isEqualTo(1L); // 마지막 메시지 ID
      assertThat(response.isHasNext()).isFalse();
      assertThat(response.getSize()).isEqualTo(3);

      verify(aiConversationRepository).existsById(chatRoomId);
      verify(aiMessageRepository)
          .findMessageHistoryByCursor(eq(chatRoomId), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("커서를 사용하여 이전 메시지를 조회한다")
    void shouldGetMessagesWithCursor() {
      // given
      Long chatRoomId = 1L;
      Long cursor = 10L;
      int size = 2;
      AiConversation conversation = AiConversationFixture.createConversation(chatRoomId);

      List<AiMessage> messages =
          List.of(
              AiMessageFixture.createAssistantMessage(9L, "AI 응답 9", conversation),
              AiMessageFixture.createUserMessage(8L, "사용자 메시지 8", conversation));

      SliceImpl<AiMessage> messageSlice = new SliceImpl<>(messages, PageRequest.of(0, size), true);

      given(aiConversationRepository.existsById(chatRoomId)).willReturn(true);
      given(aiMessageRepository.existsById(cursor)).willReturn(true);
      given(
              aiMessageRepository.findMessageHistoryByCursor(
                  eq(chatRoomId), eq(cursor), any(Pageable.class)))
          .willReturn(messageSlice);

      // when
      CursorPageResponse<ChatMessageResponse> response =
          aiMessageService.getMessageHistory(chatRoomId, cursor, size);

      // then
      assertThat(response.getContent()).hasSize(2);
      assertThat(response.getNextCursor()).isEqualTo(8L); // 마지막 메시지 ID
      assertThat(response.isHasNext()).isTrue();
      assertThat(response.getSize()).isEqualTo(2);

      verify(aiConversationRepository).existsById(chatRoomId);
      verify(aiMessageRepository)
          .findMessageHistoryByCursor(eq(chatRoomId), eq(cursor), any(Pageable.class));

      InOrder inOrder = inOrder(aiConversationRepository, aiMessageRepository);
      inOrder.verify(aiConversationRepository).existsById(chatRoomId);
      inOrder
          .verify(aiMessageRepository)
          .findMessageHistoryByCursor(eq(chatRoomId), eq(cursor), any(Pageable.class));
      verifyNoMoreInteractions(aiMessageRepository);
    }

    @Test
    @DisplayName("커서를 사용하여 마지막 메시지를 조회한다")
    void shouldGetLastMessagesWithCursor() {
      // given
      Long chatRoomId = 1L;
      Long cursor = 3L;
      int size = 2;
      AiConversation conversation = AiConversationFixture.createConversation(chatRoomId);

      List<AiMessage> messages =
          List.of(
              AiMessageFixture.createAssistantMessage(2L, "AI 응답 2", conversation),
              AiMessageFixture.createUserMessage(1L, "사용자 메시지 1", conversation));

      SliceImpl<AiMessage> messageSlice = new SliceImpl<>(messages, PageRequest.of(0, size), false);

      given(aiConversationRepository.existsById(chatRoomId)).willReturn(true);
      given(aiMessageRepository.existsById(cursor)).willReturn(true);
      given(
              aiMessageRepository.findMessageHistoryByCursor(
                  eq(chatRoomId), eq(cursor), any(Pageable.class)))
          .willReturn(messageSlice);

      // when
      CursorPageResponse<ChatMessageResponse> response =
          aiMessageService.getMessageHistory(chatRoomId, cursor, size);

      // then
      assertThat(response.getContent()).hasSize(2);
      assertThat(response.getNextCursor()).isEqualTo(1L); // 마지막 메시지 ID
      assertThat(response.isHasNext()).isFalse(); // 더 이상 없음
      assertThat(response.getSize()).isEqualTo(2);

      verify(aiConversationRepository).existsById(chatRoomId);
      verify(aiMessageRepository)
          .findMessageHistoryByCursor(eq(chatRoomId), eq(cursor), any(Pageable.class));
    }

    @Test
    @DisplayName("존재하지 않는 채팅방 ID로 조회 시 BusinessException을 발생시킨다")
    void shouldThrowExceptionWhenChatRoomNotFoundInHistory() {
      // given
      Long chatRoomId = 999L;
      int size = 50;

      given(aiConversationRepository.existsById(chatRoomId)).willReturn(false);

      // when & then
      assertThatThrownBy(() -> aiMessageService.getMessageHistory(chatRoomId, null, size))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CONVERSATION_NOT_FOUND);

      verify(aiConversationRepository).existsById(chatRoomId);
      verify(aiMessageRepository, never())
          .findMessageHistoryByCursor(any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("메시지가 없는 경우 빈 리스트를 반환한다")
    void shouldReturnEmptyListWhenNoMessages() {
      // given
      Long chatRoomId = 1L;
      int size = 50;

      SliceImpl<AiMessage> emptySlice = new SliceImpl<>(List.of(), PageRequest.of(0, size), false);

      given(aiConversationRepository.existsById(chatRoomId)).willReturn(true);
      given(
              aiMessageRepository.findMessageHistoryByCursor(
                  eq(chatRoomId), isNull(), any(Pageable.class)))
          .willReturn(emptySlice);

      // when
      CursorPageResponse<ChatMessageResponse> response =
          aiMessageService.getMessageHistory(chatRoomId, null, size);

      // then
      assertThat(response.getContent()).isEmpty();
      assertThat(response.getNextCursor()).isNull();
      assertThat(response.isHasNext()).isFalse();
      assertThat(response.getSize()).isEqualTo(0);

      verify(aiConversationRepository).existsById(chatRoomId);
      verify(aiMessageRepository)
          .findMessageHistoryByCursor(eq(chatRoomId), isNull(), any(Pageable.class));
    }
  }
}
