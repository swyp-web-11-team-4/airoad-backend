package com.swygbro.airoad.backend.ai.agent.chat.dto.request;

import java.util.List;

import lombok.Builder;

/**
 * AI 채팅 요청 DTO
 *
 * @param chatRoomId 채팅방 ID
 * @param tripPlanId 여행 계획 ID
 * @param username 유저 이름, 이메일
 * @param userPrompt 유저가 요청한 메시지
 * @param scheduledPlaceIdList 유저가 태그한 일정 장소 id list (nullable)
 */
@Builder
public record AiChatRequest(
    Long chatRoomId,
    Long tripPlanId,
    String username,
    String userPrompt,
    List<Long> scheduledPlaceIdList) {}
