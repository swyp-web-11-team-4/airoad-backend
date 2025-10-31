package com.swygbro.airoad.backend.chat.presentation.message;

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
 * <p>디버깅을 위해 WebSocket 세션의 연결, 구독, 해제 이벤트
 */
@Slf4j
@Component
public class WebSocketEventListener {

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
    String sessionId = headerAccessor.getSessionId();

    log.info("[WebSocket Event] 연결 해제 - sessionId: {}", sessionId);

    // TODO: AI API 연동 시 세션 정리 작업 추가 필요
    // - 진행 중인 AI 요청 취소 (스트리밍 응답 중단)
    // - 세션 캐시 삭제 (Redis 또는 인메모리 맵)
    // - 리소스 해제 (WebClient, 임시 파일 등)
    // 예: aiRequestManager.cancelRequest(sessionId);
    //     sessionStore.remove(sessionId);
  }
}
