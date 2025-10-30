package com.swygbro.airoad.backend.trip.domain.event;

import lombok.Builder;

/**
 * 전체 여행 일정 생성이 완료되었을 때 발행되는 이벤트입니다.
 *
 * <p>모든 일차별 일정이 생성되고 TripPlan의 isCompleted가 true로 업데이트되면 이 이벤트가 발행됩니다.
 *
 * @param chatRoomId 채팅방 ID
 * @param tripPlanId 여행 일정 ID
 * @param message    완료 메시지
 */
@Builder
public record TripPlanGenerationCompletedEvent(Long chatRoomId, Long tripPlanId, String message) {
}
