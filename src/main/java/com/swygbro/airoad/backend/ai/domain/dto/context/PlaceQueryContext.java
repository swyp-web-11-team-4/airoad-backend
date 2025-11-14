package com.swygbro.airoad.backend.ai.domain.dto.context;

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
