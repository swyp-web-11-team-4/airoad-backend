package com.swygbro.airoad.backend.ai.application.context.dto;

import lombok.Builder;

/**
 * 채팅방 기본 정보를 위한 컨텍스트
 *
 * <p>ChatRoomContextProvider에서 사용하는 데이터를 담습니다.
 *
 * @param chatRoomId 채팅방 ID
 * @param tripPlanId 여행 계획 ID
 * @param username 사용자명
 */
@Builder
public record ChatRoomContext(Long chatRoomId, Long tripPlanId, String username) {}
