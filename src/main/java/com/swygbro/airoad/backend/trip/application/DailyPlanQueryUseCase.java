package com.swygbro.airoad.backend.trip.application;

import java.util.List;

import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;

public interface DailyPlanQueryUseCase {
  /**
   * @param tripPlanId 전체 여행 계획 ID
   */
  List<DailyPlanResponse> getDailyPlanListByTripPlanId(Long tripPlanId, Long memberId);
}
