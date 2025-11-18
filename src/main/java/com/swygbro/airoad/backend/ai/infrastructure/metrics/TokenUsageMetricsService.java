package com.swygbro.airoad.backend.ai.infrastructure.metrics;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenUsageMetricsService {

  private final MeterRegistry meterRegistry;

  /**
   * 토큰 사용량을 Prometheus 카운터로 기록합니다.
   *
   * @param username 사용자 이메일
   * @param operation 작업 유형
   * @param model AI 모델명
   * @param promptTokens 입력 토큰 수
   * @param completionTokens 출력 토큰 수
   * @param totalTokens 총 토큰 수
   */
  public void recordTokenUsage(
      String username,
      String model,
      TokenUsageOperation operation,
      Integer promptTokens,
      Integer completionTokens,
      Integer totalTokens) {

    String operationValue = operation.getValue();

    if (promptTokens != null && promptTokens > 0) {
      Counter.builder("airoad.token.usage.total")
          .description("Total AI token usage by user and operation")
          .tag("username", username)
          .tag("operation", operationValue)
          .tag("model", model)
          .tag("token_type", "input")
          .register(meterRegistry)
          .increment(promptTokens.doubleValue());
    }

    if (completionTokens != null && completionTokens > 0) {
      Counter.builder("airoad.token.usage.total")
          .description("Total AI token usage by user and operation")
          .tag("username", username)
          .tag("operation", operationValue)
          .tag("model", model)
          .tag("token_type", "output")
          .register(meterRegistry)
          .increment(completionTokens.doubleValue());
    }

    if (totalTokens != null && totalTokens > 0) {
      Counter.builder("airoad.token.usage.total")
          .description("Total AI token usage by user and operation")
          .tag("username", username)
          .tag("operation", operationValue)
          .tag("model", model)
          .tag("token_type", "total")
          .register(meterRegistry)
          .increment(totalTokens.doubleValue());
    }

    log.trace(
        "토큰 메트릭 기록 완료 - username={}, operation={}, model={}, input={}, output={}, total={}",
        username,
        operationValue,
        model,
        promptTokens,
        completionTokens,
        totalTokens);
  }
}
