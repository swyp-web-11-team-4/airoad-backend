package com.swygbro.airoad.backend.ai.agent.common.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record OptimizerResponse(
    @JsonPropertyDescription("개선 전략 요약 설명, 1줄로 짧게 요약") String strategy,
    @JsonPropertyDescription("개선이 필요한 부분에 대한 설명 및 권장사항") String recommendation) {}
