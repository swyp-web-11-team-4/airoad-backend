package com.swygbro.airoad.backend.chat.application;

import org.springframework.ai.chat.messages.MessageType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageRequest;
import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageResponse;
import com.swygbro.airoad.backend.chat.domain.dto.MessageContentType;
import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.chat.domain.entity.AiMessage;
import com.swygbro.airoad.backend.chat.domain.event.AiMessageSavedEvent;
import com.swygbro.airoad.backend.chat.exception.ChatErrorCode;
import com.swygbro.airoad.backend.chat.infrastructure.AiConversationRepository;
import com.swygbro.airoad.backend.chat.infrastructure.AiMessageRepository;
import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 채팅 메시지 처리 서비스
 *
 * <p>AI와의 1:1 채팅 메시지를 처리하고 이벤트를 발행합니다.
 *
 * <h3>이벤트 기반 아키텍처</h3>
 *
 * <p>이 서비스는 메시지를 DB에d 저장한 후 {@link AiMessageSavedEvent}를 발행합니다. WebSocket 전송은 트랜잭션 커밋 후 {@link
 * AiMessageEventListener}에서 처리됩니다.
 *
 * <ul>
 *   <li><strong>DB 저장</strong>: 트랜잭션 내에서 메시지 저장
 *   <li><strong>이벤트 발행</strong>: AiMessageSavedEvent 발행
 *   <li><strong>WebSocket 전송</strong>: 트랜잭션 커밋 후 이벤트 리스너에서 처리
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiMessageService implements AiMessageUseCase {

  private final ApplicationEventPublisher eventPublisher;
  private final AiMessageRepository aiMessageRepository;
  private final AiConversationRepository aiConversationRepository;

  @Override
  @Transactional
  public void processAndSendMessage(Long chatRoomId, String userId, ChatMessageRequest request) {
    log.info("[Message] 메시지 수신 - chatRoomId: {}, userId: {}", chatRoomId, userId);

    // 1. 대화 세션 조회
    AiConversation aiConversation =
        aiConversationRepository
            .findById(chatRoomId)
            .orElseThrow(() -> new BusinessException(ChatErrorCode.CONVERSATION_NOT_FOUND));

    // 2. TEXT 메시지만 처리 (이미지, 파일 등은 향후 확장)
    if (!request.messageContentType().equals(MessageContentType.TEXT)) {
      log.warn("[Message] TEXT 타입이 아닌 메시지는 현재 지원하지 않습니다 - type: {}", request.messageContentType());
      throw new BusinessException(ChatErrorCode.INVALID_MESSAGE_FORMAT);
    }

    // 3. 사용자 메시지 저장
    AiMessage userMessage =
        AiMessage.builder()
            .messageType(MessageType.USER)
            .content(request.content())
            .conversation(aiConversation)
            .build();
    aiMessageRepository.save(userMessage);
    log.debug("[Message] 사용자 메시지 저장 완료 - messageId: {}", userMessage.getId());

    // 4. AI 응답 생성 (현재는 더미 응답)
    // TODO: 실제 LLM API 연동 후 aiService.generateResponse() 호출
    String aiMessageContent = "[AI 응답] " + request.content() + "에 대한 답변입니다.";

    // 5. AI 응답 메시지 저장
    AiMessage aiMessage =
        AiMessage.builder()
            .messageType(MessageType.ASSISTANT)
            .content(aiMessageContent)
            .conversation(aiConversation)
            .build();
    AiMessage savedAiMessage = aiMessageRepository.save(aiMessage);
    log.debug("[Message] AI 응답 메시지 저장 완료 - messageId: {}", savedAiMessage.getId());

    // 6. ChatMessageResponse로 변환
    ChatMessageResponse response = ChatMessageResponse.from(savedAiMessage);

    // 7. 이벤트 발행 (트랜잭션 커밋 후 WebSocket 전송은 이벤트 리스너에서 처리)
    eventPublisher.publishEvent(new AiMessageSavedEvent(chatRoomId, userId, response));

    log.info("[Message] 메시지 처리 완료 - chatRoomId: {}, userId: {}", chatRoomId, userId);
  }

  @Override
  @Transactional(readOnly = true)
  public CursorPageResponse<ChatMessageResponse> getMessageHistory(
      Long chatRoomId, Long cursor, int size) {
    log.info(
        "[MessageHistory] 메시지 히스토리 조회 요청 - chatRoomId: {}, cursor: {}, size: {}",
        chatRoomId,
        cursor,
        size);

    // 1. 페이지 사이즈 검증
    if (size < 1 || size > 100) {
      throw new BusinessException(ChatErrorCode.INVALID_PAGE_SIZE);
    }

    // 2. 대화 세션 존재 여부 확인
    if (!aiConversationRepository.existsById(chatRoomId)) {
      throw new BusinessException(ChatErrorCode.CONVERSATION_NOT_FOUND);
    }

    // 3. 커서가 제공된 경우, 해당 커서의 메시지가 실제로 존재하는지 검증
    if (cursor != null && !aiMessageRepository.existsById(cursor)) {
      throw new BusinessException(ChatErrorCode.INVALID_CURSOR);
    }

    // 4. 커서 기반 페이지네이션으로 메시지 조회 (최신순)
    Pageable pageable = PageRequest.of(0, size);
    Slice<AiMessage> messageSlice =
        aiMessageRepository.findMessageHistoryByCursor(chatRoomId, cursor, pageable);

    // 5. AiMessage를 ChatMessageResponse로 변환
    var responseList = messageSlice.map(ChatMessageResponse::from).getContent();

    // 6. 다음 커서 계산 (마지막 메시지의 ID를 다음 커서로 사용)
    Long nextCursor =
        responseList.isEmpty() ? null : responseList.get(responseList.size() - 1).id();

    // 7. 다음 페이지 존재 여부
    boolean hasNext = messageSlice.hasNext();

    log.info(
        "[MessageHistory] 메시지 히스토리 조회 완료 - chatRoomId: {}, 조회된 메시지 수: {}, hasNext: {}",
        chatRoomId,
        responseList.size(),
        hasNext);

    // 8. CursorPageResponse로 변환하여 반환
    return CursorPageResponse.of(responseList, nextCursor, hasNext);
  }
}
