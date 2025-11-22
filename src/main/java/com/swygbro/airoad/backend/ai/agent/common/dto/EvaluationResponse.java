package com.swygbro.airoad.backend.ai.agent.common.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record EvaluationResponse<E extends Enum<E>>(
    @JsonPropertyDescription("평가 결과") Evaluation evaluation,
    @JsonPropertyDescription("발견된 이슈 목록") List<Issue<E>> issues) {
  public enum Evaluation {
    PASS,
    NEEDS_IMPROVEMENT
  }

  public record Issue<E extends Enum<E>>(
      @JsonPropertyDescription("이슈 유형") E type,
      @JsonPropertyDescription("이슈 내용, 1줄로 요약") String message) {}
}
