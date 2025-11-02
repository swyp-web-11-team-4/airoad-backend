package com.swygbro.airoad.backend.chat.domain.event;

import lombok.Builder;

/**
 * AI 서버로 메시지 전송 요청 이벤트
 *
 * @param chatRoomId 채팅방 ID (AiConversation.id)
 * @param tripPlanId 여행 계획 ID (TripPlan.id)
 * @param username 사용자 ID (이메일)
 * @param userMessage AI가 생성한 메시지
 */
@Builder
public record AiChatRequestedEvent(
    Long chatRoomId, Long tripPlanId, String username, String userMessage) {}
