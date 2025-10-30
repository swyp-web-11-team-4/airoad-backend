package com.swygbro.airoad.backend.ai.agent;

import com.swygbro.airoad.backend.ai.domain.dto.AiResponseContentType;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;

public interface AiroadAgent {

  /** 에이전트 식별자 */
  String name();

  /** 각 에이전트의 Intent 매칭 여부 */
  boolean supports(AiResponseContentType type);

  /**
   * @param data 요청 데이터
   * @return LLM 응답 데이터를 반환
   */
  CommonResponse<String> execute(Object data);
}
