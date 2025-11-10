package com.swygbro.airoad.backend.ai.domain.event;

import java.util.List;

import lombok.Builder;

@Builder
public record PlaceSummaryGeneratedEvent(
    Long placeId, String name, String address, List<String> themes, String content) {}
