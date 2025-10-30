package com.swygbro.airoad.backend.trip.domain.event;

import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;
import lombok.Builder;

@Builder
public record DailyPlanSavedEvent(
    Long chatRoomId,
    Long tripPlanId,
    DailyPlanResponse dailyPlan) {

}
