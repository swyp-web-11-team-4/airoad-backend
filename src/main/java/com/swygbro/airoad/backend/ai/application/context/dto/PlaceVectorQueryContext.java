package com.swygbro.airoad.backend.ai.application.context.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record PlaceVectorQueryContext(
    String region, List<String> themes, int topK, double similarityThreshold) {}
