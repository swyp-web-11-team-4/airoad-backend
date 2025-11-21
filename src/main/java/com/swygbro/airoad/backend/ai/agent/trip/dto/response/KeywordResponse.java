package com.swygbro.airoad.backend.ai.agent.trip.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.Builder;

@Builder
public record KeywordResponse(
    @JsonPropertyDescription("장소 관련 키워드") List<String> places,
    @JsonPropertyDescription("음식점 관련 키워드") List<String> restaurants) {}
