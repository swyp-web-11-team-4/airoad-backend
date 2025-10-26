package com.swygbro.airoad.backend.websocket.application;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.common.domain.dto.ErrorResponse;
import com.swygbro.airoad.backend.common.exception.WebSocketErrorCode;
import com.swygbro.airoad.backend.websocket.domain.event.AiResponseReceivedEvent;

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

  /**
   * AI 응답 수신 이벤트 처리
   *
   * <p>AI 서버로부터 응답(청크)을 받으면 다음과 같이 처리합니다:
   *
   * <ol>
   *   <li><strong>WebSocket 전송</strong>: 모든 청크를 즉시 클라이언트에게 전송 (실시간 스트리밍)
   * </ol>
   *
   * @param event AI 응답 수신 이벤트
   */
  @Async
  @EventListener
  public void handleAiResponseReceived(AiResponseReceivedEvent event) {
    log.debug(
        "[AI Response] 처리 시작 - chatRoomId: {}, tripPlanId: {}, contentType: {}, isComplete: {}",
        event.chatRoomId(),
        event.tripPlanId(),
        event.contentType(),
        event.isComplete());

    sendToWebSocket(event);
  }

  /**
   * WebSocket을 통해 클라이언트에게 응답 전송
   *
   * <p>전송 실패 시 클라이언트에게 에러 메시지를 전송하여 스트리밍 중단을 알립니다.
   *
   * @param event AI 응답 수신 이벤트
   */
  private void sendToWebSocket(AiResponseReceivedEvent event) {
    try {
      String destination = determineDestination(event);
      messagingTemplate.convertAndSendToUser(event.userId(), destination, event.content());

      log.debug(
          "[WebSocket] 전송 성공 - chatRoomId: {}, destination: {}, isComplete: {}",
          event.chatRoomId(),
          destination,
          event.isComplete());
    } catch (Exception e) {
      log.error(
          "[WebSocket] 전송 실패 - chatRoomId: {}, tripPlanId: {}, error: {}",
          event.chatRoomId(),
          event.tripPlanId(),
          e.getMessage(),
          e);

      // 클라이언트에게 전송 실패 알림
      sendErrorToClient(event.userId(), event.chatRoomId(), e);
    }
  }

  /**
   * WebSocket 전송 실패 시 클라이언트에게 에러 메시지 전송
   *
   * <p>/user/sub/errors/{chatRoomId} 채널로 에러를 전송하여 클라이언트가 스트리밍 중단을 인지할 수 있도록 합니다.
   *
   * @param userId 사용자 ID
   * @param chatRoomId 채팅방 ID (null이면 "unknown" 사용)
   * @param originalException 원본 예외
   */
  private void sendErrorToClient(String userId, Long chatRoomId, Exception originalException) {
    try {
      ErrorResponse errorResponse =
          ErrorResponse.of(
              WebSocketErrorCode.MESSAGE_SEND_FAILED.getCode(),
              "AI 응답 전송에 실패했습니다. 새로고침 후 다시 시도해주세요.",
              "/ws-stomp");

      // chatRoomId가 있으면 해당 채팅방의 에러 채널로 전송
      String destination = chatRoomId != null ? "/sub/errors/" + chatRoomId : "/sub/errors/unknown";
      messagingTemplate.convertAndSendToUser(userId, destination, errorResponse);

      log.info("[WebSocket] 에러 메시지 전송 완료, destination: {}", destination);
    } catch (Exception e) {
      // 에러 전송마저 실패한 경우 로그만 남김
      log.error("[WebSocket] 에러 메시지 전송 실패 - error: {}", e.getMessage(), e);
    }
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
