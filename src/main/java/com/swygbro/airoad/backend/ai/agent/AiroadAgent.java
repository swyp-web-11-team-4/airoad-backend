package com.swygbro.airoad.backend.ai.agent;

public interface AiroadAgent {

  /**
   * 각 에이전트의 식별자 매칭 여부
   *
   * @param agentName 에이전트 식별자
   * @return 지원 여부
   */
  boolean supports(String agentName);

  /**
   * @param data 요청 데이터
   */
  void execute(Object data);
}
