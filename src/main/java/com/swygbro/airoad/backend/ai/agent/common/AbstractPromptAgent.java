package com.swygbro.airoad.backend.ai.agent.common;

import com.swygbro.airoad.backend.ai.application.query.AiPromptTemplateQueryUseCase;
import com.swygbro.airoad.backend.ai.domain.dto.response.AiPromptTemplateResponse;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.entity.PromptType;

/**
 * AI 에이전트의 공통 프롬프트 조회 로직을 제공하는 추상 클래스
 *
 * <p>모든 에이전트가 DB에서 활성화된 프롬프트를 조회할 때 사용하는 공통 로직을 제공합니다.
 */
public abstract class AbstractPromptAgent implements AiroadAgent {

  protected final AiPromptTemplateQueryUseCase promptTemplateQueryUseCase;

  protected AbstractPromptAgent(AiPromptTemplateQueryUseCase promptTemplateQueryUseCase) {
    this.promptTemplateQueryUseCase = promptTemplateQueryUseCase;
  }

  /**
   * 활성화된 SYSTEM 프롬프트를 조회합니다.
   *
   * @param agentType 에이전트 타입
   * @return SYSTEM 프롬프트 문자열
   */
  protected String findActiveSystemPrompt(AgentType agentType) {
    AiPromptTemplateResponse response =
        promptTemplateQueryUseCase.findActivePromptTemplate(PromptType.SYSTEM, agentType);

    return response.prompt();
  }

  /**
   * 활성화된 USER 프롬프트를 조회합니다.
   *
   * @param agentType 에이전트 타입
   * @return USER 프롬프트 문자열
   */
  protected String findActiveUserPrompt(AgentType agentType) {
    AiPromptTemplateResponse response =
        promptTemplateQueryUseCase.findActivePromptTemplate(PromptType.USER, agentType);

    return response.prompt();
  }

  /**
   * 활성화된 SYSTEM, USER 프롬프트를 모두 조회합니다.
   *
   * @param agentType 에이전트 타입
   * @return PromptPair (SYSTEM, USER 프롬프트 쌍)
   */
  protected PromptPair findActivePromptPair(AgentType agentType) {
    String systemPrompt = findActiveSystemPrompt(agentType);
    String userPrompt = findActiveUserPrompt(agentType);

    return new PromptPair(systemPrompt, userPrompt);
  }

  /** SYSTEM, USER 프롬프트 쌍을 담는 레코드 */
  protected record PromptPair(String systemPrompt, String userPrompt) {}
}
