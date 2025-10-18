package com.swygbro.airoad.backend.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * STOMP Principal Handshake Handler
 *
 * <p>WebSocket 연결 시 Spring Security Context에서 Principal을 가져와 사용자를 식별합니다.
 * <p>인증된 사용자만 WebSocket 연결이 가능합니다.
 *
 * <p><strong>개발/테스트 환경</strong>: Principal이 없으면 임시 UUID 생성 (프로덕션에서는 제거 필요)
 */
@Slf4j
public class StompPrincipalHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {

        // Spring Security Context에서 Principal 가져오기
        Principal principal = request.getPrincipal();
        if (principal != null) {
            log.info("[Principal] 인증된 사용자: {}", principal.getName());
            return principal;
        }

        // TODO: 프로덕션 환경에서는 아래 코드 제거하고 예외 발생시키기
        // throw new IllegalStateException("인증되지 않은 사용자는 WebSocket에 연결할 수 없습니다.");

        // ===== 개발/테스트용 임시 코드 (프로덕션 배포 전 삭제) =====
        String userId = "user-" + UUID.randomUUID().toString().substring(0, 8);
        log.warn("[Principal] 테스트용 임시 Principal 생성: {}", userId);
        return new StompPrincipal(userId);
        // ================================================================
    }

    /**
     * STOMP Principal 구현체
     */
    private static class StompPrincipal implements Principal {
        private final String name;

        public StompPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
