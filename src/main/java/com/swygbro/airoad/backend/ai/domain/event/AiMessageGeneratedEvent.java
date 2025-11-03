package com.swygbro.airoad.backend.ai.domain.event;

import lombok.Builder;

/**
 * AI 채팅 메시지 생성 완료 이벤트
 *
 * @param chatRoomId 채팅방 ID
 * @param tripPlanId 여행 계획 ID
 * @param username 사용자 이름 (이메일)
 * @param aiMessage AI가 생성한 메시지
 */
@Builder
public record AiMessageGeneratedEvent(
    Long chatRoomId, Long tripPlanId, String username, String aiMessage) {}
