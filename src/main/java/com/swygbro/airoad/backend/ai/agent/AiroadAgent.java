package com.swygbro.airoad.backend.ai.agent;

import com.swygbro.airoad.backend.ai.domain.dto.AiResponseContentType;

public interface AiroadAgent {

  /** 에이전트 식별자 */
  String name();

  /** 각 에이전트의 Intent 매칭 여부 */
  boolean supports(AiResponseContentType type);

  /**
   * @param data 요청 데이터
   */
  void execute(Object data);
}
