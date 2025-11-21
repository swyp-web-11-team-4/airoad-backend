package com.swygbro.airoad.backend.ai.agent.common.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.Builder;

@Builder
public record OrchestratorResponse<E extends Enum<E>>(
    @JsonPropertyDescription("작업 계획 설명") String analysis,
    @JsonPropertyDescription("작업 목록") List<WorkerTask<E>> workerTasks) {}
