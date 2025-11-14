package com.swygbro.airoad.backend.ai.agent.summary.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.Builder;

@Builder
public record PlaceSummaryAiResponse(
    @JsonPropertyDescription("자연어 문단. 완전한 문장으로만 구성. 지역명 2-3회 포함. 2-3개 문단, 150-250단어.")
        String content) {}
