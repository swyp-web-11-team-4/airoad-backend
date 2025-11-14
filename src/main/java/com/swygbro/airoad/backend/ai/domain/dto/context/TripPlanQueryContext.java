package com.swygbro.airoad.backend.ai.domain.dto.context;

import lombok.Builder;

/**
 * 여행 계획 조회를 위한 Query 컨텍스트
 *
 * <p>TripPlanQueryContextProvider에서 사용하는 데이터를 담습니다.
 *
 * <p>CQRS 패턴의 Query 측면을 담당하며, 기존 여행 계획을 조회하여 요약 정보를 제공합니다.
 *
 * @param tripPlanId 여행 계획 ID
 * @param username 사용자명
 */
@Builder
public record TripPlanQueryContext(Long tripPlanId, String username) {}
