package com.swygbro.airoad.backend.ai.application.context.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record PlaceQueryContext(
    String name,
    String address,
    String description,
    String operatingHours,
    String holidayInfo,
    List<String> themes) {}
