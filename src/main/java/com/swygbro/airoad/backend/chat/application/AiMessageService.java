package com.swygbro.airoad.backend.chat.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageRequest;
import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageResponse;
import com.swygbro.airoad.backend.chat.domain.dto.MessageContentType;
import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.chat.domain.entity.AiMessage;
import com.swygbro.airoad.backend.chat.exception.ChatErrorCode;
import com.swygbro.airoad.backend.chat.infrastructure.AiConversationRepository;
import com.swygbro.airoad.backend.chat.infrastructure.AiMessageRepository;
import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.websocket.domain.event.AiRequestEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 채팅 메시지 처리 서비스
 *
 * <p>AI와의 1:1 채팅 메시지를 처리하는 서비스입니다.
 *
 * <h3>메시지 처리 흐름</h3>
 *
 * <ol>
 *   <li><strong>사용자 메시지 저장</strong>: 클라이언트로부터 받은 메시지를 DB에 저장
 *   <li><strong>AI 서버 전송</strong>: AI 서버에 메시지 전송 (구현 예정)
 *   <li><strong>AI 응답 수신</strong>: AI 서버로부터 응답을 이벤트로 수신
 *   <li><strong>WebSocket 전송</strong>: {@link
 *       com.swygbro.airoad.backend.chat.application.AiResponseEventListener}에서 클라이언트로 실시간 전송
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiMessageService implements AiMessageUseCase {

  private final AiMessageRepository aiMessageRepository;
  private final AiConversationRepository aiConversationRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public void processAndSendMessage(Long chatRoomId, String userId, ChatMessageRequest request) {
    log.info("[Message] 메시지 수신 - chatRoomId: {}, userId: {}", chatRoomId, userId);

    // 1. 대화 세션 조회
    AiConversation aiConversation =
        aiConversationRepository
            .findById(chatRoomId)
            .orElseThrow(() -> new BusinessException(ChatErrorCode.CONVERSATION_NOT_FOUND));

    // 2. 권한 검증 - 해당 사용자가 채팅방 소유자인지 확인
    if (!aiConversation.isOwner(userId)) {
      log.warn("[Message] 채팅방 접근 권한 없음 - chatRoomId: {}, userId: {}", chatRoomId, userId);
      throw new BusinessException(ChatErrorCode.CONVERSATION_ACCESS_DENIED);
    }

    // 3. TEXT 메시지만 처리 (이미지, 파일 등은 향후 확장)
    if (request.messageContentType() != MessageContentType.TEXT) {
      log.warn("[Message] TEXT 타입이 아닌 메시지는 현재 지원하지 않습니다 - type: {}", request.messageContentType());
      throw new BusinessException(ChatErrorCode.INVALID_MESSAGE_FORMAT);
    }

    // 4. AI 서버에 메시지 전송 요청 이벤트 발행
    Long tripPlanId = aiConversation.getTripPlanId();
    AiRequestEvent aiRequestEvent =
        new AiRequestEvent(chatRoomId, tripPlanId, userId, request.content());

    eventPublisher.publishEvent(aiRequestEvent);

    log.info(
        "[Message] AI 요청 이벤트 발행 완료 - chatRoomId: {}, tripPlanId: {}, userId: {}",
        chatRoomId,
        tripPlanId,
        userId);

    // AI 응답은 AiResponseReceivedEvent로 수신되어 AiResponseEventListener에서 WebSocket으로 전송됨
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

    // 3. 커서가 제공된 경우, 해당 커서의 메시지가 해당 대화방에 속하는지 검증
    if (cursor != null && !aiMessageRepository.existsByIdAndConversationId(cursor, chatRoomId)) {
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
