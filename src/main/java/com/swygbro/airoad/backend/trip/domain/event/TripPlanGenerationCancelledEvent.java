package com.swygbro.airoad.backend.trip.domain.event;

import lombok.Builder;

/**
 * 여행 일정 생성이 취소되었을 때 발행되는 이벤트입니다.
 *
 * <p>사용자가 진행 중인 일정 생성을 중단하거나, 세션이 종료되어 취소될 때 이 이벤트가 발행됩니다.
 *
 * @param chatRoomId 채팅방 ID
 * @param tripPlanId 여행 일정 ID (생성되었을 경우)
 * @param reason 취소 사유
 */
@Builder
public record TripPlanGenerationCancelledEvent(Long chatRoomId, Long tripPlanId, String reason) {}
