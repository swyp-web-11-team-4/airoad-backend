package com.swygbro.airoad.backend.ai.application.query;

import com.swygbro.airoad.backend.ai.domain.dto.response.AiPromptTemplateResponse;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.entity.PromptType;
import com.swygbro.airoad.backend.common.domain.dto.PageResponse;

/** AI 프롬프트 템플릿을 조회하는 Query(조회) 관련 비즈니스 로직을 정의하는 UseCase 인터페이스 */
public interface AiPromptTemplateQueryUseCase {

  PageResponse<AiPromptTemplateResponse> findPromptTemplates(
      int page, int size, String sort, String order);

  /**
   * 특정 AI 프롬프트 템플릿을 ID로 조회합니다.
   *
   * @param promptId 조회할 템플릿의 ID
   * @return 조회된 템플릿 정보
   */
  AiPromptTemplateResponse findPromptTemplate(Long promptId);

  /**
   * 활성화된 특정 AI 프롬프트 템플릿을 조회합니다.
   *
   * @param promptType 프롬프트 타입 (USER 또는 SYSTEM)
   * @param agentType 에이전트 타입 (예: TRIP_AGENT, CHAT_AGENT 등)
   * @return 활성화된 프롬프트 템플릿의 정보 (AiPromptTemplateResponse)
   */
  AiPromptTemplateResponse findActivePromptTemplate(PromptType promptType, AgentType agentType);
}
