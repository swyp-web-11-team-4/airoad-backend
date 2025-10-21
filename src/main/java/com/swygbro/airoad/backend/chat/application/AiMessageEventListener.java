package com.swygbro.airoad.backend.chat.application;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.swygbro.airoad.backend.chat.domain.event.AiMessageSavedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 메시지 이벤트 리스너
 *
 * <p>AI 메시지 저장 이벤트를 처리하여 WebSocket으로 메시지를 전송합니다.
 *
 * <h3>트랜잭션 이벤트 리스너</h3>
 *
 * <p>{@code @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)}를 사용하여 트랜잭션 커밋 후에
 * WebSocket 전송을 수행합니다. 이를 통해 다음을 보장합니다:
 *
 * <ul>
 *   <li>DB 저장이 완전히 완료된 후에만 WebSocket 메시지 전송
 *   <li>트랜잭션 롤백 시 WebSocket 전송 방지
 *   <li>WebSocket 전송 실패가 트랜잭션에 영향을 주지 않음
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiMessageEventListener {

  private final SimpMessagingTemplate messagingTemplate;

  /**
   * AI 메시지 저장 완료 이벤트 처리
   *
   * <p>트랜잭션 커밋 후 WebSocket을 통해 사용자에게 AI 응답을 전송합니다.
   *
   * @param event AI 메시지 저장 이벤트
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleAiMessageSaved(AiMessageSavedEvent event) {
    log.debug(
        "[WebSocket] AI 메시지 전송 시작 - chatRoomId: {}, userId: {}",
        event.chatRoomId(),
        event.userId());

    try {
      // WebSocket을 통해 사용자의 특정 채팅방 구독 경로로 AI 응답 전송
      // 실제 경로: /user/{userId}/sub/chat/{chatRoomId}
      String destination = "/sub/chat/" + event.chatRoomId();
      messagingTemplate.convertAndSendToUser(event.userId(), destination, event.response());

      log.info(
          "[WebSocket] AI 응답 전송 완료 - chatRoomId: {}, userId: {}",
          event.chatRoomId(),
          event.userId());

    } catch (Exception e) {
      // WebSocket 전송 실패는 로그만 남기고 트랜잭션에 영향을 주지 않음
      log.error(
          "[WebSocket] AI 응답 전송 실패 - chatRoomId: {}, userId: {}, error: {}",
          event.chatRoomId(),
          event.userId(),
          e.getMessage(),
          e);
    }
  }
}
