package com.swygbro.airoad.backend.ai.agent.trip.dto.response;

import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;

import lombok.Builder;

/**
 * DistanceCalculationStep의 출력
 *
 * <p>거리 계산이 완료된 장소 정보 (summary는 ScheduleSummaryResponse에서 조회)
 */
@Builder
public record DistanceCalculationResponse(
    Long placeId, int visitOrder, ScheduledCategory category, int travelTime) {}
