package com.swygbro.airoad.backend.ai.domain.dto.context;

import java.util.List;

import lombok.Builder;

@Builder
public record PlaceVectorQueryContext(
    String region, List<String> themes, int topK, double similarityThreshold) {}
