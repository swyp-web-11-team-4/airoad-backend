package com.swygbro.airoad.backend.ai.agent.common.evaluator;

import java.util.List;

import com.swygbro.airoad.backend.ai.agent.common.context.WorkerContext;

/** 도메인 무관 평가자 인터페이스 */
public interface Evaluator<C extends WorkerContext, E extends Enum<E>> {

  /**
   * 컨텍스트를 평가하여 결과 반환
   *
   * @param context 평가할 컨텍스트
   * @return 평가 결과
   */
  EvaluationResult<E> evaluate(C context);

  record EvaluationResult<E extends Enum<E>>(Evaluation evaluation, List<Issue<E>> issues) {

    public enum Evaluation {
      PASS,
      NEEDS_IMPROVEMENT
    }

    public record Issue<E extends Enum<E>>(E type, String message) {}
  }
}
