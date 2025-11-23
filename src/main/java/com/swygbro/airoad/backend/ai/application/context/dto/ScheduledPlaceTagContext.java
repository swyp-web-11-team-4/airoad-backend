package com.swygbro.airoad.backend.ai.application.context.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record ScheduledPlaceTagContext(
    Long tripPlanId, String username, List<Long> scheduledPlaceIdList) {}
