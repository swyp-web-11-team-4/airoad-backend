package com.swygbro.airoad.backend.chat.application;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
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
import com.swygbro.airoad.backend.chat.domain.event.AiChatRequestedEvent;
import com.swygbro.airoad.backend.chat.exception.ChatErrorCode;
import com.swygbro.airoad.backend.chat.fixture.AiConversationFixture;
import com.swygbro.airoad.backend.chat.fixture.AiMessageFixture;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiConversationRepository;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiMessageRepository;
import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AiMessageServiceTest {

  @Mock private AiMessageRepository aiMessageRepository;

  @Mock private AiConversationRepository aiConversationRepository;

  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private AiMessageService aiMessageService;

  @Captor ArgumentCaptor<AiChatRequestedEvent> eventCaptor;

  @Nested
  @DisplayName("processAndSendMessage 메서드는")
  class ProcessAndSendMessage {

    @Test
    @DisplayName("TEXT 메시지를 정상적으로 처리하고 AI 요청 이벤트를 발행한다")
    void shouldProcessTextMessageAndPublishEvent() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 100L;
      String userId = "user123@example.com";
      String messageContent = "서울 3박 4일 여행 계획을 짜주세요";
      ChatMessageRequest request = new ChatMessageRequest(messageContent, MessageContentType.TEXT);

      AiConversation conversation =
          AiConversationFixture.createConversation(chatRoomId, userId, tripPlanId);
      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));

      // when
      aiMessageService.processAndSendMessage(chatRoomId, userId, request);

      // then
      verify(aiConversationRepository).findById(chatRoomId);
      verify(eventPublisher).publishEvent(eventCaptor.capture());

      AiChatRequestedEvent publishedEvent = eventCaptor.getValue();
      assertThat(publishedEvent.chatRoomId()).isEqualTo(chatRoomId);
      assertThat(publishedEvent.tripPlanId()).isEqualTo(tripPlanId);
      assertThat(publishedEvent.username()).isEqualTo(userId);
      assertThat(publishedEvent.userMessage()).isEqualTo(messageContent);

      verifyNoMoreInteractions(aiConversationRepository, eventPublisher);
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

      AiConversation conversation = AiConversationFixture.createConversation(chatRoomId, userId);
      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));

      // when & then
      assertThatThrownBy(() -> aiMessageService.processAndSendMessage(chatRoomId, userId, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.INVALID_MESSAGE_FORMAT);

      verify(aiConversationRepository).findById(chatRoomId);
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("채팅방 소유자가 아닌 사용자의 메시지는 BusinessException을 발생시킨다")
    void shouldThrowExceptionWhenUserIsNotOwner() {
      // given
      Long chatRoomId = 1L;
      String ownerId = "owner123";
      String otherUserId = "other456";
      ChatMessageRequest request = new ChatMessageRequest("안녕하세요", MessageContentType.TEXT);

      AiConversation conversation = AiConversationFixture.createConversation(chatRoomId, ownerId);
      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));

      // when & then
      assertThatThrownBy(
              () -> aiMessageService.processAndSendMessage(chatRoomId, otherUserId, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CONVERSATION_ACCESS_DENIED);

      verify(aiConversationRepository).findById(chatRoomId);
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("발견한 채팅방에 대응하는 TripPlanId가 없으면 BusinessException을 발생시킨다")
    void shouldThrowExceptionWhenTripPlanIsNotFounded() {
      // given
      Long chatRoomId = 1L;
      String ownerId = "owner123";
      ChatMessageRequest request = new ChatMessageRequest("안녕하세요", MessageContentType.TEXT);

      AiConversation conversation =
          AiConversationFixture.createConversation(chatRoomId, ownerId, null);
      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));

      // when & then
      assertThatThrownBy(() -> aiMessageService.processAndSendMessage(chatRoomId, ownerId, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.INVALID_CONVERSATION_FORMAT);

      verify(aiConversationRepository).findById(chatRoomId);
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
      String userId = "user123";
      int size = 50;
      AiConversation conversation = AiConversationFixture.createConversation(chatRoomId, userId);

      List<AiMessage> messages =
          List.of(
              AiMessageFixture.createAssistantMessage(3L, "AI 응답 3", conversation),
              AiMessageFixture.createUserMessage(2L, "사용자 메시지 2", conversation),
              AiMessageFixture.createAssistantMessage(1L, "AI 응답 1", conversation));

      SliceImpl<AiMessage> messageSlice = new SliceImpl<>(messages, PageRequest.of(0, size), false);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));
      given(
              aiMessageRepository.findMessageHistoryByCursor(
                  eq(chatRoomId), isNull(), any(Pageable.class)))
          .willReturn(messageSlice);

      // when
      CursorPageResponse<ChatMessageResponse> response =
          aiMessageService.getMessageHistory(chatRoomId, userId, null, size);

      // then
      assertThat(response.getContent()).hasSize(3);
      assertThat(response.getNextCursor()).isEqualTo(1L); // 마지막 메시지 ID
      assertThat(response.isHasNext()).isFalse();
      assertThat(response.getSize()).isEqualTo(3);

      verify(aiConversationRepository).findById(chatRoomId);
      verify(aiMessageRepository)
          .findMessageHistoryByCursor(eq(chatRoomId), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("커서를 사용하여 이전 메시지를 조회한다")
    void shouldGetMessagesWithCursor() {
      // given
      Long chatRoomId = 1L;
      String userId = "user123";
      Long cursor = 10L;
      int size = 2;
      AiConversation conversation = AiConversationFixture.createConversation(chatRoomId, userId);

      List<AiMessage> messages =
          List.of(
              AiMessageFixture.createAssistantMessage(9L, "AI 응답 9", conversation),
              AiMessageFixture.createUserMessage(8L, "사용자 메시지 8", conversation));

      SliceImpl<AiMessage> messageSlice = new SliceImpl<>(messages, PageRequest.of(0, size), true);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));
      given(aiMessageRepository.existsByIdAndConversationId(cursor, chatRoomId)).willReturn(true);
      given(
              aiMessageRepository.findMessageHistoryByCursor(
                  eq(chatRoomId), eq(cursor), any(Pageable.class)))
          .willReturn(messageSlice);

      // when
      CursorPageResponse<ChatMessageResponse> response =
          aiMessageService.getMessageHistory(chatRoomId, userId, cursor, size);

      // then
      assertThat(response.getContent()).hasSize(2);
      assertThat(response.getNextCursor()).isEqualTo(8L); // 마지막 메시지 ID
      assertThat(response.isHasNext()).isTrue();
      assertThat(response.getSize()).isEqualTo(2);

      verify(aiConversationRepository).findById(chatRoomId);
      verify(aiMessageRepository)
          .findMessageHistoryByCursor(eq(chatRoomId), eq(cursor), any(Pageable.class));

      InOrder inOrder = inOrder(aiConversationRepository, aiMessageRepository);
      inOrder.verify(aiConversationRepository).findById(chatRoomId);
      inOrder.verify(aiMessageRepository).existsByIdAndConversationId(cursor, chatRoomId);
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
      String userId = "user123";
      Long cursor = 3L;
      int size = 2;
      AiConversation conversation = AiConversationFixture.createConversation(chatRoomId, userId);

      List<AiMessage> messages =
          List.of(
              AiMessageFixture.createAssistantMessage(2L, "AI 응답 2", conversation),
              AiMessageFixture.createUserMessage(1L, "사용자 메시지 1", conversation));

      SliceImpl<AiMessage> messageSlice = new SliceImpl<>(messages, PageRequest.of(0, size), false);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));
      given(aiMessageRepository.existsByIdAndConversationId(cursor, chatRoomId)).willReturn(true);
      given(
              aiMessageRepository.findMessageHistoryByCursor(
                  eq(chatRoomId), eq(cursor), any(Pageable.class)))
          .willReturn(messageSlice);

      // when
      CursorPageResponse<ChatMessageResponse> response =
          aiMessageService.getMessageHistory(chatRoomId, userId, cursor, size);

      // then
      assertThat(response.getContent()).hasSize(2);
      assertThat(response.getNextCursor()).isEqualTo(1L); // 마지막 메시지 ID
      assertThat(response.isHasNext()).isFalse(); // 더 이상 없음
      assertThat(response.getSize()).isEqualTo(2);

      verify(aiConversationRepository).findById(chatRoomId);
      verify(aiMessageRepository)
          .findMessageHistoryByCursor(eq(chatRoomId), eq(cursor), any(Pageable.class));
    }

    @Test
    @DisplayName("존재하지 않는 채팅방 ID로 조회 시 BusinessException을 발생시킨다")
    void shouldThrowExceptionWhenChatRoomNotFoundInHistory() {
      // given
      Long chatRoomId = 999L;
      String userId = "user123";
      int size = 50;

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> aiMessageService.getMessageHistory(chatRoomId, userId, null, size))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CONVERSATION_NOT_FOUND);

      verify(aiConversationRepository).findById(chatRoomId);
      verify(aiMessageRepository, never())
          .findMessageHistoryByCursor(any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("메시지가 없는 경우 빈 리스트를 반환한다")
    void shouldReturnEmptyListWhenNoMessages() {
      // given
      Long chatRoomId = 1L;
      String userId = "user123";
      int size = 50;
      AiConversation conversation = AiConversationFixture.createConversation(chatRoomId, userId);

      SliceImpl<AiMessage> emptySlice = new SliceImpl<>(List.of(), PageRequest.of(0, size), false);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));
      given(
              aiMessageRepository.findMessageHistoryByCursor(
                  eq(chatRoomId), isNull(), any(Pageable.class)))
          .willReturn(emptySlice);

      // when
      CursorPageResponse<ChatMessageResponse> response =
          aiMessageService.getMessageHistory(chatRoomId, userId, null, size);

      // then
      assertThat(response.getContent()).isEmpty();
      assertThat(response.getNextCursor()).isNull();
      assertThat(response.isHasNext()).isFalse();
      assertThat(response.getSize()).isEqualTo(0);

      verify(aiConversationRepository).findById(chatRoomId);
      verify(aiMessageRepository)
          .findMessageHistoryByCursor(eq(chatRoomId), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("다른 대화방의 메시지 ID를 커서로 사용하면 BusinessException을 발생시킨다")
    void shouldThrowExceptionWhenCursorBelongsToDifferentConversation() {
      // given
      Long chatRoomId = 1L;
      String userId = "user123";
      Long cursorFromOtherRoom = 999L; // 다른 대화방의 메시지 ID
      int size = 50;
      AiConversation conversation = AiConversationFixture.createConversation(chatRoomId, userId);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));
      given(aiMessageRepository.existsByIdAndConversationId(cursorFromOtherRoom, chatRoomId))
          .willReturn(false);

      // when & then
      assertThatThrownBy(
              () ->
                  aiMessageService.getMessageHistory(chatRoomId, userId, cursorFromOtherRoom, size))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.INVALID_CURSOR);

      verify(aiConversationRepository).findById(chatRoomId);
      verify(aiMessageRepository).existsByIdAndConversationId(cursorFromOtherRoom, chatRoomId);
      verify(aiMessageRepository, never())
          .findMessageHistoryByCursor(any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("채팅방 소유자가 아닌 사용자가 조회 시 BusinessException을 발생시킨다")
    void shouldThrowExceptionWhenUserIsNotOwnerInHistory() {
      // given
      Long chatRoomId = 1L;
      String ownerId = "owner123";
      String otherUserId = "other456";
      int size = 50;

      AiConversation conversation = AiConversationFixture.createConversation(chatRoomId, ownerId);
      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));

      // when & then
      assertThatThrownBy(
              () -> aiMessageService.getMessageHistory(chatRoomId, otherUserId, null, size))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CONVERSATION_ACCESS_DENIED);

      verify(aiConversationRepository).findById(chatRoomId);
      verify(aiMessageRepository, never())
          .findMessageHistoryByCursor(any(), any(), any(Pageable.class));
    }
  }
}
