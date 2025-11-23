package com.swygbro.airoad.backend.ai.agent.common.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.Builder;

@Builder
public record WorkerTask<E extends Enum<E>>(
    @JsonPropertyDescription("작업 유형") E type,
    @JsonPropertyDescription("작업 설명") String description) {}
