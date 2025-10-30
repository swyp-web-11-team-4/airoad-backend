package com.swygbro.airoad.backend.common.domain.event;

import com.swygbro.airoad.backend.common.domain.dto.ErrorResponse;

/**
 * WebSocket 에러 전송 이벤트
 *
 * <p>WebSocket 인터셉터에서 발생한 에러를 에러 채널로 전송하기 위한 이벤트입니다.
 *
 * <p>이벤트 기반 아키텍처를 통해 {@code JwtWebSocketInterceptor}와 {@code SimpMessagingTemplate} 간의 순환 참조를
 * 해결합니다.
 *
 * @param userId 사용자 ID (이메일)
 * @param errorChannel 에러 채널 경로 (예: /sub/errors/123)
 * @param errorResponse 에러 응답 DTO
 */
public record WebSocketErrorEvent(
    String userId, String errorChannel, ErrorResponse errorResponse) {}
