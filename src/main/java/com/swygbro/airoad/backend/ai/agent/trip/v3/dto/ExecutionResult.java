package com.swygbro.airoad.backend.ai.agent.trip.v3.dto;

import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse;

public record ExecutionResult(AiDailyPlanResponse plan, String selectedDistrict) {

  public static ExecutionResult of(AiDailyPlanResponse plan, String selectedDistrict) {
    return new ExecutionResult(plan, selectedDistrict);
  }
}
