package com.swygbro.airoad.backend.trip.domain.event;

import lombok.Builder;

/**
 * AI가 발행하는 일정 수정 시작 메세지 이벤트
 *
 * @param chatRoomId 채팅방 ID (AiConversation.id)
 * @param tripPlanId 여행 계획 ID (TripPlan.id)
 * @param username 유저 이메일
 * @param message 보낼 메세지
 */
@Builder
public record TripPlanUpdateStartedEvent(
    Long chatRoomId, Long tripPlanId, String username, String message) {}
