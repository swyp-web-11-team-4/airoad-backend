package com.swygbro.airoad.backend.trip.domain.event;

import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;

import lombok.Builder;

/**
 * 여행 일정 생성이 요청되었을 때 발행되는 이벤트입니다.
 *
 * <p>사용자가 여행 일정 생성을 요청하면 이 이벤트가 발행되고, AI 리스너가 이를 수신하여 일정 생성 프로세스를 시작합니다.
 *
 * @param chatRoomId 채팅방 ID
 * @param tripPlanId 여행 계획 ID
 * @param request 여행 일정 생성 요청 정보
 */
@Builder
public record TripPlanGenerationRequestedEvent(
    Long chatRoomId, Long tripPlanId, TripPlanCreateRequest request) {}
