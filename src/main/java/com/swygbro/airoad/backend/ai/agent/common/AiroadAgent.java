package com.swygbro.airoad.backend.ai.agent.common;

import com.swygbro.airoad.backend.ai.domain.entity.AgentType;

public interface AiroadAgent {

  /**
   * 각 에이전트의 식별자 매칭 여부
   *
   * @param agentType 에이전트 타입
   * @return 지원 여부
   */
  boolean supports(AgentType agentType);

  /**
   * @param data 요청 데이터
   */
  void execute(Object data);
}
