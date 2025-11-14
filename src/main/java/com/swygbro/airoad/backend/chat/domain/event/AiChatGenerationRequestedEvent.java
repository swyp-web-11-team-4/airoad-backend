package com.swygbro.airoad.backend.chat.domain.event;

import lombok.Builder;

/**
 * AI 서버로 메시지 전송 요청 이벤트
 *
 * @param chatRoomId 채팅방 ID (AiConversation.id)
 * @param tripPlanId 여행 계획 ID (TripPlan.id)
 * @param username 유저 이름, 이메일 (User.username)
 * @param userMessage 유저가 요청한 메시지
 * @param scheduledPlaceId 유저가 태그한 일정 장소 id (nullable)
 */
@Builder
public record AiChatGenerationRequestedEvent(
    Long chatRoomId, Long tripPlanId, String username, String userMessage, Long scheduledPlaceId) {}
