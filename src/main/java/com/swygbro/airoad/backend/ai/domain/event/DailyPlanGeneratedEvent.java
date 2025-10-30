package com.swygbro.airoad.backend.ai.domain.event;

import com.swygbro.airoad.backend.trip.domain.dto.request.DailyPlanCreateRequest;
import lombok.Builder;

/**
 * 일차별 여행 일정이 생성되었을 때 발행되는 이벤트입니다.
 *
 * <p>AI가 하루치 일정을 완성하면 이 이벤트가 발행되고, WebSocket을 통해 클라이언트로 실시간 전송됩니다.
 *
 * @param chatRoomId 채팅방 ID
 * @param tripPlanId 여행 일정 ID
 * @param dailyPlan  생성된 일차별 일정 데이터
 */
@Builder
public record DailyPlanGeneratedEvent(Long chatRoomId, Long tripPlanId, DailyPlanCreateRequest dailyPlan) {
}
