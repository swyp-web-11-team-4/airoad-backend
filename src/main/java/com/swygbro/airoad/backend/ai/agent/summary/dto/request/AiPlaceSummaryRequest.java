package com.swygbro.airoad.backend.ai.agent.summary.dto.request;

import java.util.List;

import lombok.Builder;

@Builder
public record AiPlaceSummaryRequest(
    Long placeId, String name, String address, String description, List<String> themes) {}
