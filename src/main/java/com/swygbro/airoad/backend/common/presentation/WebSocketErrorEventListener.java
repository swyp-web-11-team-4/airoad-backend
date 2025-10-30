package com.swygbro.airoad.backend.common.presentation;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.common.domain.event.WebSocketErrorEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 에러 이벤트 리스너
 *
 * <p>{@link WebSocketErrorEvent}를 수신하여 클라이언트에게 에러 메시지를 전송합니다.
 *
 * <p>이벤트 기반 아키텍처를 통해 {@code JwtWebSocketInterceptor}와 {@code SimpMessagingTemplate} 간의 순환 참조를
 * 해결합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketErrorEventListener {

  private final SimpMessagingTemplate messagingTemplate;

  /**
   * WebSocket 에러 이벤트를 처리합니다.
   *
   * <p>에러 메시지를 사용자별 에러 채널로 전송합니다.
   *
   * @param event WebSocket 에러 이벤트
   */
  @EventListener
  public void handleWebSocketError(WebSocketErrorEvent event) {
    try {
      messagingTemplate.convertAndSendToUser(
          event.userId(), event.errorChannel(), event.errorResponse());

      log.info(
          "[WebSocket] 에러 이벤트 처리 완료 - userId: {}, channel: {}, code: {}",
          event.userId(),
          event.errorChannel(),
          event.errorResponse().code());

    } catch (Exception e) {
      log.error(
          "[WebSocket] 에러 이벤트 처리 실패 - userId: {}, error: {}", event.userId(), e.getMessage(), e);
    }
  }
}
