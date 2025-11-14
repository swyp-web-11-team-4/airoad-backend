package com.swygbro.airoad.backend.trip.domain.event;

import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;

import lombok.Builder;

/**
 * Ai가 발행하는 일정 수정 완료 이벤트
 *
 * <p>일정 수정이 완료되면 여기에 데이터를 담아서 리스너로 보냅니다
 *
 * @param chatRoomId 채팅방 ID
 * @param tripPlanId 여행 계획 ID
 * @param username 사용자 이름 (이메일)
 * @param dailyPlan 수정된 일차 일정 데이터
 */
@Builder
public record TripPlanUpdatedEvent(
    Long chatRoomId, Long tripPlanId, String username, DailyPlanResponse dailyPlan) {}
