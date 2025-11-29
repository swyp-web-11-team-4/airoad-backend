package com.swygbro.airoad.backend.ai.agent.trip.pipeline;

import com.swygbro.airoad.backend.ai.agent.trip.context.ExecutionContext;

/**
 * 파이프라인의 단일 Step을 정의하는 인터페이스
 *
 * <p>ExecutionContext를 받아서 수정 후 반환합니다. 메서드 체이닝을 통해 동적 파이프라인 실행을 지원합니다.
 */
public interface PipelineStep {

  /**
   * Step을 실행하고 ExecutionContext를 반환
   *
   * @param context 실행 컨텍스트
   * @return 수정된 ExecutionContext
   */
  ExecutionContext execute(ExecutionContext context);

  /**
   * 이 Step의 이름을 반환
   *
   * @return Step의 설명적 이름
   */
  String getName();
}
