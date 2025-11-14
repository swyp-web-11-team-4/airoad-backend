package com.swygbro.airoad.backend.chat.application;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.swygbro.airoad.backend.chat.domain.dto.request.ChatMessageRequest;
import com.swygbro.airoad.backend.chat.domain.dto.response.ChatMessageResponse;
import com.swygbro.airoad.backend.chat.domain.dto.response.MessageContentType;
import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.chat.domain.entity.AiMessage;
import com.swygbro.airoad.backend.chat.domain.event.AiChatGenerationRequestedEvent;
import com.swygbro.airoad.backend.chat.exception.ChatErrorCode;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiConversationRepository;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiMessageRepository;
import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.fixture.chat.AiConversationFixture;
import com.swygbro.airoad.backend.fixture.chat.AiMessageFixture;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.fixture.trip.TripPlanFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.trip.application.ScheduledPlaceCommandUseCase;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AiMessageServiceTest {

  @Mock private AiMessageRepository aiMessageRepository;

  @Mock private AiConversationRepository aiConversationRepository;

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private ScheduledPlaceCommandUseCase scheduledPlaceCommandUseCase;

  @InjectMocks private AiMessageService aiMessageService;

  @Nested
  @DisplayName("사용자가 AI 채팅 메시지를 전송할 때")
  class ProcessAndSendMessageTests {

    @Test
    @DisplayName("채팅방 소유자가 텍스트 메시지를 전송하면 AI 요청 이벤트를 발행한다")
    void 채팅방_소유자의_텍스트_메시지_전송_시_AI_요청_이벤트_발행() {
      // given
      Member member = MemberFixture.create();
      TripPlan tripPlan = TripPlanFixture.create();
      ReflectionTestUtils.setField(tripPlan, "id", 100L);

      AiConversation conversation =
          AiConversationFixture.createWithMemberAndTripPlan(member, tripPlan);

      ChatMessageRequest request =
          new ChatMessageRequest("서울 3박 4일 여행 추천해주세요", MessageContentType.TEXT, 123L);
      String userEmail = member.getEmail();
      Long chatRoomId = 1L;

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));
      given(
              scheduledPlaceCommandUseCase.validateScheduledPlace(
                  userEmail, request.scheduledPlaceId()))
          .willReturn(true);

      // when
      aiMessageService.processAndSendMessage(chatRoomId, userEmail, request);

      // then
      ArgumentCaptor<AiChatGenerationRequestedEvent> eventCaptor =
          ArgumentCaptor.forClass(AiChatGenerationRequestedEvent.class);
      verify(eventPublisher).publishEvent(eventCaptor.capture());

      AiChatGenerationRequestedEvent publishedEvent = eventCaptor.getValue();
      assertThat(publishedEvent.chatRoomId()).isEqualTo(chatRoomId);
      assertThat(publishedEvent.tripPlanId()).isEqualTo(100L);
      assertThat(publishedEvent.username()).isEqualTo(userEmail);
      assertThat(publishedEvent.userMessage()).isEqualTo(request.content());
    }

    @Test
    @DisplayName("존재하지 않는 채팅방에 메시지를 전송하면 예외가 발생한다")
    void 존재하지_않는_채팅방에_메시지_전송_시_예외_발생() {
      // given
      Long chatRoomId = 999L;
      String userEmail = "test@example.com";
      ChatMessageRequest request = new ChatMessageRequest("메시지 내용", MessageContentType.TEXT, 123L);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(
              () -> aiMessageService.processAndSendMessage(chatRoomId, userEmail, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CONVERSATION_NOT_FOUND);

      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("채팅방 소유자가 아닌 사용자가 메시지를 전송하면 예외가 발생한다")
    void 채팅방_소유자가_아닌_사용자의_메시지_전송_시_예외_발생() {
      // given
      Member owner = MemberFixture.create();
      AiConversation conversation = AiConversationFixture.createWithMember(owner);
      Long chatRoomId = 1L;
      String unauthorizedEmail = "unauthorized@example.com";
      ChatMessageRequest request = new ChatMessageRequest("메시지 내용", MessageContentType.TEXT, 123L);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));

      // when & then
      assertThatThrownBy(
              () -> aiMessageService.processAndSendMessage(chatRoomId, unauthorizedEmail, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CONVERSATION_ACCESS_DENIED);

      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("TEXT 타입이 아닌 메시지를 전송하면 예외가 발생한다")
    void TEXT_타입이_아닌_메시지_전송_시_예외_발생() {
      // given
      Member member = MemberFixture.create();
      AiConversation conversation = AiConversationFixture.createWithMember(member);
      Long chatRoomId = 1L;
      ChatMessageRequest request = new ChatMessageRequest("이미지 내용", MessageContentType.IMAGE, 123L);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));

      // when & then
      assertThatThrownBy(
              () -> aiMessageService.processAndSendMessage(chatRoomId, member.getEmail(), request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.INVALID_MESSAGE_FORMAT);

      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("빈 내용의 메시지를 전송하면 예외가 발생한다")
    void 빈_내용의_메시지_전송_시_예외_발생() {
      // given
      Member member = MemberFixture.create();
      AiConversation conversation = AiConversationFixture.createWithMember(member);
      Long chatRoomId = 1L;
      ChatMessageRequest request = new ChatMessageRequest("", MessageContentType.TEXT, null);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));

      // when & then
      assertThatThrownBy(
              () -> aiMessageService.processAndSendMessage(chatRoomId, member.getEmail(), request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.INVALID_MESSAGE_FORMAT);

      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("공백만 있는 메시지를 전송하면 예외가 발생한다")
    void 공백만_있는_메시지_전송_시_예외_발생() {
      // given
      Member member = MemberFixture.create();
      AiConversation conversation = AiConversationFixture.createWithMember(member);
      Long chatRoomId = 1L;
      ChatMessageRequest request = new ChatMessageRequest("   ", MessageContentType.TEXT, 123L);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));

      // when & then
      assertThatThrownBy(
              () -> aiMessageService.processAndSendMessage(chatRoomId, member.getEmail(), request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.INVALID_MESSAGE_FORMAT);

      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("여행 계획이 연결되지 않은 채팅방에 메시지를 전송하면 예외가 발생한다")
    void 여행_계획이_없는_채팅방에_메시지_전송_시_예외_발생() {
      // given
      Member member = MemberFixture.create();
      AiConversation conversationMock = mock(AiConversation.class);

      Long chatRoomId = 1L;
      ChatMessageRequest request = new ChatMessageRequest("메시지 내용", MessageContentType.TEXT, 123L);

      given(aiConversationRepository.findById(chatRoomId))
          .willReturn(Optional.of(conversationMock));
      given(conversationMock.isOwner(member.getEmail())).willReturn(true);
      given(conversationMock.getTripPlanId()).willReturn(null);

      // when & then
      assertThatThrownBy(
              () -> aiMessageService.processAndSendMessage(chatRoomId, member.getEmail(), request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.INVALID_CONVERSATION_FORMAT);

      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("scheduledPlaceId가 null인 경우 장소 검증을 건너뛰고 AI 요청 이벤트를 발행한다")
    void scheduledPlaceId가_null인_경우_장소_검증_건너뛰고_이벤트_발행() {
      // given
      Member member = MemberFixture.create();
      TripPlan tripPlan = TripPlanFixture.create();
      ReflectionTestUtils.setField(tripPlan, "id", 100L);

      AiConversation conversation =
          AiConversationFixture.createWithMemberAndTripPlan(member, tripPlan);

      ChatMessageRequest request =
          new ChatMessageRequest("서울 3박 4일 여행 추천해주세요", MessageContentType.TEXT, null);
      String userEmail = member.getEmail();
      Long chatRoomId = 1L;

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));

      // when
      aiMessageService.processAndSendMessage(chatRoomId, userEmail, request);

      // then
      verify(scheduledPlaceCommandUseCase, never()).validateScheduledPlace(any(), any());

      ArgumentCaptor<AiChatGenerationRequestedEvent> eventCaptor =
          ArgumentCaptor.forClass(AiChatGenerationRequestedEvent.class);
      verify(eventPublisher).publishEvent(eventCaptor.capture());

      AiChatGenerationRequestedEvent publishedEvent = eventCaptor.getValue();
      assertThat(publishedEvent.chatRoomId()).isEqualTo(chatRoomId);
      assertThat(publishedEvent.tripPlanId()).isEqualTo(100L);
      assertThat(publishedEvent.username()).isEqualTo(userEmail);
      assertThat(publishedEvent.userMessage()).isEqualTo(request.content());
      assertThat(publishedEvent.scheduledPlaceId()).isNull();
    }
  }

  @Nested
  @DisplayName("사용자가 채팅 히스토리를 조회할 때")
  class GetMessageHistoryTests {

    @Test
    @DisplayName("채팅방 소유자는 최초 페이지 메시지 히스토리를 조회할 수 있다")
    void 채팅방_소유자의_최초_페이지_메시지_히스토리_조회() {
      // given
      Member member = MemberFixture.create();
      AiConversation conversation = AiConversationFixture.createWithMember(member);
      Long chatRoomId = 1L;
      int size = 20;

      AiMessage message1 =
          AiMessageFixture.createWithConversationAndContent(conversation, "첫 번째 메시지");
      ReflectionTestUtils.setField(message1, "id", 10L);
      ReflectionTestUtils.setField(message1, "createdAt", LocalDateTime.now());

      AiMessage message2 =
          AiMessageFixture.createWithConversationAndContent(conversation, "두 번째 메시지");
      ReflectionTestUtils.setField(message2, "id", 20L);
      ReflectionTestUtils.setField(message2, "createdAt", LocalDateTime.now());

      Pageable pageable = Pageable.ofSize(size);
      Slice<AiMessage> messageSlice = new SliceImpl<>(List.of(message1, message2), pageable, false);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));
      given(aiMessageRepository.findMessageHistoryByCursor(eq(chatRoomId), isNull(), any()))
          .willReturn(messageSlice);

      // when
      CursorPageResponse<ChatMessageResponse> result =
          aiMessageService.getMessageHistory(chatRoomId, member.getEmail(), null, size);

      // then
      assertThat(result.getContent()).hasSize(2);
      assertThat(result.isHasNext()).isFalse();
      assertThat(result.getNextCursor()).isEqualTo(20L);
      verify(aiMessageRepository).findMessageHistoryByCursor(eq(chatRoomId), isNull(), any());
    }

    @Test
    @DisplayName("커서를 사용하여 다음 페이지 메시지를 조회할 수 있다")
    void 커서를_사용한_다음_페이지_메시지_조회() {
      // given
      Member member = MemberFixture.create();
      AiConversation conversation = AiConversationFixture.createWithMember(member);
      Long chatRoomId = 1L;
      Long cursor = 10L;
      int size = 20;

      AiMessage message = AiMessageFixture.createWithConversationAndContent(conversation, "메시지 내용");
      ReflectionTestUtils.setField(message, "id", 5L);
      ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.now());

      Pageable pageable = Pageable.ofSize(size);
      Slice<AiMessage> messageSlice = new SliceImpl<>(List.of(message), pageable, true);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));
      given(aiMessageRepository.existsByIdAndConversationId(cursor, chatRoomId)).willReturn(true);
      given(aiMessageRepository.findMessageHistoryByCursor(eq(chatRoomId), eq(cursor), any()))
          .willReturn(messageSlice);

      // when
      CursorPageResponse<ChatMessageResponse> result =
          aiMessageService.getMessageHistory(chatRoomId, member.getEmail(), cursor, size);

      // then
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.isHasNext()).isTrue();
      assertThat(result.getNextCursor()).isEqualTo(5L);
      verify(aiMessageRepository).existsByIdAndConversationId(cursor, chatRoomId);
    }

    @Test
    @DisplayName("메시지가 없는 채팅방에서 빈 목록을 반환한다")
    void 메시지가_없는_채팅방_조회_시_빈_목록_반환() {
      // given
      Member member = MemberFixture.create();
      AiConversation conversation = AiConversationFixture.createWithMember(member);
      Long chatRoomId = 1L;
      int size = 20;

      Pageable pageable = Pageable.ofSize(size);
      Slice<AiMessage> emptySlice = new SliceImpl<>(Collections.emptyList(), pageable, false);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));
      given(aiMessageRepository.findMessageHistoryByCursor(eq(chatRoomId), isNull(), any()))
          .willReturn(emptySlice);

      // when
      CursorPageResponse<ChatMessageResponse> result =
          aiMessageService.getMessageHistory(chatRoomId, member.getEmail(), null, size);

      // then
      assertThat(result.getContent()).isEmpty();
      assertThat(result.isHasNext()).isFalse();
      assertThat(result.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 채팅방 히스토리 조회 시 예외가 발생한다")
    void 존재하지_않는_채팅방_히스토리_조회_시_예외_발생() {
      // given
      Long chatRoomId = 999L;
      String userEmail = "test@example.com";
      int size = 20;

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(
              () -> aiMessageService.getMessageHistory(chatRoomId, userEmail, null, size))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CONVERSATION_NOT_FOUND);
    }

    @Test
    @DisplayName("채팅방 소유자가 아닌 사용자가 히스토리 조회 시 예외가 발생한다")
    void 채팅방_소유자가_아닌_사용자의_히스토리_조회_시_예외_발생() {
      // given
      Member owner = MemberFixture.create();
      AiConversation conversation = AiConversationFixture.createWithMember(owner);
      Long chatRoomId = 1L;
      String unauthorizedEmail = "unauthorized@example.com";
      int size = 20;

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));

      // when & then
      assertThatThrownBy(
              () -> aiMessageService.getMessageHistory(chatRoomId, unauthorizedEmail, null, size))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CONVERSATION_ACCESS_DENIED);
    }

    @Test
    @DisplayName("페이지 크기가 1보다 작으면 예외가 발생한다")
    void 페이지_크기가_1보다_작으면_예외_발생() {
      // given
      Member member = MemberFixture.create();
      Long chatRoomId = 1L;
      int invalidSize = 0;

      // when & then
      assertThatThrownBy(
              () ->
                  aiMessageService.getMessageHistory(
                      chatRoomId, member.getEmail(), null, invalidSize))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.INVALID_PAGE_SIZE);
    }

    @Test
    @DisplayName("페이지 크기가 100보다 크면 예외가 발생한다")
    void 페이지_크기가_100보다_크면_예외_발생() {
      // given
      Member member = MemberFixture.create();
      Long chatRoomId = 1L;
      int invalidSize = 101;

      // when & then
      assertThatThrownBy(
              () ->
                  aiMessageService.getMessageHistory(
                      chatRoomId, member.getEmail(), null, invalidSize))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.INVALID_PAGE_SIZE);
    }

    @Test
    @DisplayName("유효하지 않은 커서로 조회 시 예외가 발생한다")
    void 유효하지_않은_커서로_조회_시_예외_발생() {
      // given
      Member member = MemberFixture.create();
      AiConversation conversation = AiConversationFixture.createWithMember(member);
      Long chatRoomId = 1L;
      Long invalidCursor = 999L;
      int size = 20;

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));
      given(aiMessageRepository.existsByIdAndConversationId(invalidCursor, chatRoomId))
          .willReturn(false);

      // when & then
      assertThatThrownBy(
              () ->
                  aiMessageService.getMessageHistory(
                      chatRoomId, member.getEmail(), invalidCursor, size))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.INVALID_CURSOR);

      verify(aiMessageRepository).existsByIdAndConversationId(invalidCursor, chatRoomId);
    }

    @Test
    @DisplayName("다른 채팅방에 속한 커서로 조회 시 예외가 발생한다")
    void 다른_채팅방의_커서로_조회_시_예외_발생() {
      // given
      Member member = MemberFixture.create();
      AiConversation conversation = AiConversationFixture.createWithMember(member);
      Long chatRoomId = 1L;
      Long cursorFromAnotherRoom = 5L;
      int size = 20;

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));
      given(aiMessageRepository.existsByIdAndConversationId(cursorFromAnotherRoom, chatRoomId))
          .willReturn(false);

      // when & then
      assertThatThrownBy(
              () ->
                  aiMessageService.getMessageHistory(
                      chatRoomId, member.getEmail(), cursorFromAnotherRoom, size))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.INVALID_CURSOR);
    }

    @Test
    @DisplayName("경계값 페이지 크기 1로 조회할 수 있다")
    void 경계값_페이지_크기_1로_조회() {
      // given
      Member member = MemberFixture.create();
      AiConversation conversation = AiConversationFixture.createWithMember(member);
      Long chatRoomId = 1L;
      int size = 1;

      AiMessage message = AiMessageFixture.createWithConversation(conversation);
      ReflectionTestUtils.setField(message, "id", 1L);
      ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.now());

      Pageable pageable = Pageable.ofSize(size);
      Slice<AiMessage> messageSlice = new SliceImpl<>(List.of(message), pageable, false);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));
      given(aiMessageRepository.findMessageHistoryByCursor(eq(chatRoomId), isNull(), any()))
          .willReturn(messageSlice);

      // when
      CursorPageResponse<ChatMessageResponse> result =
          aiMessageService.getMessageHistory(chatRoomId, member.getEmail(), null, size);

      // then
      assertThat(result.getContent()).hasSize(1);
      verify(aiMessageRepository)
          .findMessageHistoryByCursor(eq(chatRoomId), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("경계값 페이지 크기 100으로 조회할 수 있다")
    void 경계값_페이지_크기_100으로_조회() {
      // given
      Member member = MemberFixture.create();
      AiConversation conversation = AiConversationFixture.createWithMember(member);
      Long chatRoomId = 1L;
      int size = 100;

      Pageable pageable = Pageable.ofSize(size);
      Slice<AiMessage> messageSlice = new SliceImpl<>(Collections.emptyList(), pageable, false);

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));
      given(aiMessageRepository.findMessageHistoryByCursor(eq(chatRoomId), isNull(), any()))
          .willReturn(messageSlice);

      // when
      CursorPageResponse<ChatMessageResponse> result =
          aiMessageService.getMessageHistory(chatRoomId, member.getEmail(), null, size);

      // then
      assertThat(result.getContent()).isEmpty();
      verify(aiMessageRepository)
          .findMessageHistoryByCursor(eq(chatRoomId), isNull(), any(Pageable.class));
    }
  }
}
