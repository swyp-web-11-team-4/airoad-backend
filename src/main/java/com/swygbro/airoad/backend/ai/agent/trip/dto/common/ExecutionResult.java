package com.swygbro.airoad.backend.ai.agent.trip.dto.common;

import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse;

public record ExecutionResult(AiDailyPlanResponse plan, String selectedDistrict) {

  public static ExecutionResult of(AiDailyPlanResponse plan, String selectedDistrict) {
    return new ExecutionResult(plan, selectedDistrict);
  }
}
