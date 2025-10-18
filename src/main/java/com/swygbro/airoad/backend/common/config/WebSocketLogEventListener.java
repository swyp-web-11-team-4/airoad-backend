package com.swygbro.airoad.backend.common.config;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 연결/구독 이벤트 리스너
 *
 * <p>디버깅을 위해 WebSocket 세션의 연결, 구독, 해제 이벤트를 로깅합니다.
 */
@Slf4j
@Component
public class WebSocketLogEventListener {

  @EventListener
  public void handleWebSocketConnectListener(SessionConnectEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    log.info("[WebSocket Event] 연결 시도 - sessionId: {}", headerAccessor.getSessionId());
  }

  @EventListener
  public void handleWebSocketConnectedListener(SessionConnectedEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    log.info("[WebSocket Event] 연결 완료 - sessionId: {}", headerAccessor.getSessionId());
  }

  @EventListener
  public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = headerAccessor.getSessionId();
    String destination = headerAccessor.getDestination();
    String subscriptionId = headerAccessor.getSubscriptionId();

    log.info(
        "[WebSocket Event] 구독 - sessionId: {}, destination: {}, subscriptionId: {}",
        sessionId,
        destination,
        subscriptionId);
  }

  @EventListener
  public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    log.info("[WebSocket Event] 연결 해제 - sessionId: {}", headerAccessor.getSessionId());
  }
}
