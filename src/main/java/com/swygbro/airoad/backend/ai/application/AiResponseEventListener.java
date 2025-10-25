package com.swygbro.airoad.backend.ai.application;

import org.springframework.ai.chat.messages.MessageType;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.ai.domain.dto.AiResponseContentType;
import com.swygbro.airoad.backend.ai.domain.event.AiResponseReceivedEvent;
import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.chat.domain.entity.AiMessage;
import com.swygbro.airoad.backend.chat.exception.ChatErrorCode;
import com.swygbro.airoad.backend.chat.infrastructure.AiConversationRepository;
import com.swygbro.airoad.backend.chat.infrastructure.AiMessageRepository;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 응답 이벤트 리스너
 *
 * <p>AI 서버로부터 응답을 받았을 때 WebSocket을 통해 클라이언트에게 실시간으로 전달합니다.
 *
 * <h3>스트리밍 처리 방식</h3>
 *
 * <ul>
 *   <li>AI 응답이 청크 단위로 수신될 때마다 즉시 WebSocket으로 전송
 *   <li>비동기 처리로 메시지 저장 로직과 분리
 *   <li>클라이언트는 실시간으로 AI 응답을 받아 UI에 표시 가능
 * </ul>
 *
 * <h3>채널 라우팅</h3>
 *
 * <ul>
 *   <li><strong>CHAT</strong>: {@code /user/sub/chat/{chatRoomId}} - 일반 채팅 메시지 (chatRoomId =
 *       AiConversation ID)
 *   <li><strong>SCHEDULE</strong>: {@code /user/sub/schedule/{tripPlanId}} - 여행 일정 데이터 (tripPlanId
 *       = TripPlan ID)
 * </ul>
 *
 * <p><strong>참고</strong>: CHAT은 chatRoomId(AiConversation.id), SCHEDULE은 tripPlanId(TripPlan.id)를
 * 사용합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiResponseEventListener {

  private final SimpMessagingTemplate messagingTemplate;
  private final AiMessageRepository aiMessageRepository;
  private final AiConversationRepository aiConversationRepository;

  /**
   * AI 응답 수신 이벤트 처리
   *
   * <p>AI 서버로부터 응답(청크)을 받으면 다음과 같이 처리합니다:
   *
   * <ol>
   *   <li><strong>WebSocket 전송</strong>: 모든 청크를 즉시 클라이언트에게 전송 (실시간 스트리밍)
   *   <li><strong>DB 저장</strong>: 완료된 응답만 저장
   *       <ul>
   *         <li>CHAT: AiMessage 테이블에 저장
   *         <li>SCHEDULE: 향후 TripPlan 관련 테이블에 저장 (TODO)
   *       </ul>
   * </ol>
   *
   * @param event AI 응답 수신 이벤트
   */
  @Async
  @EventListener
  public void handleAiResponseReceived(AiResponseReceivedEvent event) {
    log.debug(
        "[AI Response] 처리 시작 - chatRoomId: {}, tripPlanId: {}, userId: {}, contentType: {}, isComplete: {}",
        event.chatRoomId(),
        event.tripPlanId(),
        event.userId(),
        event.contentType(),
        event.isComplete());

    // 1. WebSocket 전송 (모든 청크 즉시 전송)
    sendToWebSocket(event);

    // 2. DB 저장 (완료된 메시지만)
    if (event.isComplete()) {
      saveToDatabase(event);
    }
  }

  /**
   * WebSocket을 통해 클라이언트에게 응답 전송
   *
   * @param event AI 응답 수신 이벤트
   */
  private void sendToWebSocket(AiResponseReceivedEvent event) {
    try {
      String destination = determineDestination(event);
      messagingTemplate.convertAndSendToUser(event.userId(), destination, event.content());

      log.debug(
          "[WebSocket] 전송 성공 - userId: {}, destination: {}, isComplete: {}",
          event.userId(),
          destination,
          event.isComplete());
    } catch (Exception e) {
      log.error(
          "[WebSocket] 전송 실패 - chatRoomId: {}, tripPlanId: {}, userId: {}, error: {}",
          event.chatRoomId(),
          event.tripPlanId(),
          event.userId(),
          e.getMessage(),
          e);
    }
  }

  /**
   * 완료된 AI 응답을 DB에 저장
   *
   * @param event AI 응답 수신 이벤트
   */
  private void saveToDatabase(AiResponseReceivedEvent event) {
    if (event.contentType() == AiResponseContentType.CHAT) {
      saveChatMessage(event);
    } else if (event.contentType() == AiResponseContentType.SCHEDULE) {
      saveSchedule(event);
    }
  }

  /**
   * CHAT 타입 응답을 AiMessage 테이블에 저장
   *
   * @param event AI 응답 수신 이벤트
   */
  @Transactional
  private void saveChatMessage(AiResponseReceivedEvent event) {
    try {
      AiConversation aiConversation =
          aiConversationRepository
              .findById(event.chatRoomId())
              .orElseThrow(() -> new BusinessException(ChatErrorCode.CONVERSATION_NOT_FOUND));

      AiMessage aiMessage =
          AiMessage.builder()
              .messageType(MessageType.ASSISTANT)
              .content(event.content())
              .conversation(aiConversation)
              .build();
      aiMessageRepository.save(aiMessage);

      log.info(
          "[DB] CHAT 메시지 저장 완료 - chatRoomId: {}, messageId: {}",
          event.chatRoomId(),
          aiMessage.getId());
    } catch (Exception e) {
      log.error(
          "[DB] CHAT 메시지 저장 실패 - chatRoomId: {}, userId: {}, error: {}",
          event.chatRoomId(),
          event.userId(),
          e.getMessage(),
          e);
    }
  }

  /**
   * SCHEDULE 타입 응답을 TripPlan 관련 테이블에 저장
   *
   * <p>TODO: 여행 일정 저장 로직 구현 필요
   *
   * @param event AI 응답 수신 이벤트
   */
  private void saveSchedule(AiResponseReceivedEvent event) {
    log.info(
        "[DB] SCHEDULE 저장 시작 - tripPlanId: {}, userId: {} (TODO: 구현 필요)",
        event.tripPlanId(),
        event.userId());
    // TODO: TripPlan 관련 엔티티에 일정 데이터 저장
  }

  /**
   * contentType에 따라 WebSocket 목적지 경로를 결정합니다.
   *
   * @param event AI 응답 수신 이벤트
   * @return WebSocket 목적지 경로
   */
  private String determineDestination(AiResponseReceivedEvent event) {
    return switch (event.contentType()) {
      case CHAT -> "/sub/chat/" + event.chatRoomId();
      case SCHEDULE -> "/sub/schedule/" + event.tripPlanId();
    };
  }
}
