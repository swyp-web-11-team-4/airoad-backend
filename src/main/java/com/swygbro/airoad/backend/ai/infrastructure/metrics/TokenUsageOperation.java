package com.swygbro.airoad.backend.ai.infrastructure.metrics;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AI 토큰 사용량 기록 시 작업 유형을 정의하는 Enum
 *
 * <p>Prometheus 메트릭의 operation 태그 값으로 사용됩니다.
 */
@Getter
@RequiredArgsConstructor
public enum TokenUsageOperation {
  CHAT_EDIT("chat_edit", "채팅을 통한 일정 편집"),
  TRIP_GENERATION("trip_generation", "여행 일정 생성");

  private final String value;
  private final String description;
}
