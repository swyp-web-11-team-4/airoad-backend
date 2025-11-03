package com.swygbro.airoad.backend.chat.presentation.message;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.domain.event.AiMessageGeneratedEvent;
import com.swygbro.airoad.backend.chat.domain.dto.ChatStreamDto;
import com.swygbro.airoad.backend.common.domain.dto.ErrorResponse;
import com.swygbro.airoad.backend.common.exception.WebSocketErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatNotificationListener {

  private final SimpMessagingTemplate messagingTemplate;

  /**
   * AI 메시지 생성 완료 이벤트 처리
   *
   * <p>AI 서버로부터 생성된 메시지를 수신하여 WebSocket으로 전송합니다.
   *
   * @param event AI 메시지 생성 이벤트
   */
  @EventListener
  public void handleAiMessageGenerated(AiMessageGeneratedEvent event) {
    log.debug(
        "[Chat Notification] AI 메시지 생성 완료 - chatRoomId: {}, username: {}",
        event.chatRoomId(),
        event.username());

    sendToWebSocket(event);
  }

  /**
   * WebSocket을 통해 클라이언트에게 응답 전송
   *
   * <p>전송 실패 시 클라이언트에게 에러 메시지를 전송합니다.
   *
   * @param event AI 메시지 생성 이벤트
   */
  private void sendToWebSocket(AiMessageGeneratedEvent event) {
    try {
      // ChatStreamDto로 변환 (완료된 메시지)
      ChatStreamDto response = ChatStreamDto.of(event.aiMessage(), true);

      // WebSocket 목적지 결정
      String destination = "/sub/chat/" + event.chatRoomId();

      // 사용자에게 전송
      messagingTemplate.convertAndSendToUser(event.username(), destination, response);

      log.debug(
          "[Chat Notification] WebSocket 전송 성공 - chatRoomId: {}, destination: {}",
          event.chatRoomId(),
          destination);
    } catch (Exception e) {
      log.error(
          "[Chat Notification] WebSocket 전송 실패 - chatRoomId: {}, error: {}",
          event.chatRoomId(),
          e.getMessage(),
          e);

      // 클라이언트에게 전송 실패 알림
      sendErrorToClient(event.username(), event.chatRoomId());
    }
  }

  /**
   * WebSocket 전송 실패 시 클라이언트에게 에러 메시지 전송
   *
   * <p>/user/sub/errors/{chatRoomId} 채널로 에러를 전송하여 클라이언트가 스트리밍 중단을 인지할 수 있도록 합니다.
   *
   * @param username 사용자 이름, 이메일
   * @param chatRoomId 채팅방 ID (null이면 "unknown" 사용)
   */
  private void sendErrorToClient(String username, Long chatRoomId) {
    try {
      ErrorResponse errorResponse =
          ErrorResponse.of(
              WebSocketErrorCode.MESSAGE_SEND_FAILED.getCode(),
              "AI 응답 전송에 실패했습니다. 새로고침 후 다시 시도해주세요.",
              "/ws-stomp");

      // chatRoomId가 있으면 해당 채팅방의 에러 채널로 전송
      String destination = chatRoomId != null ? "/sub/errors/" + chatRoomId : "/sub/errors/unknown";
      messagingTemplate.convertAndSendToUser(username, destination, errorResponse);

      log.info("[WebSocket] 에러 메시지 전송 완료, destination: {}", destination);
    } catch (Exception e) {
      // 에러 전송마저 실패한 경우 로그만 남김
      log.error("[WebSocket] 에러 메시지 전송 실패 - error: {}", e.getMessage(), e);
    }
  }
}
