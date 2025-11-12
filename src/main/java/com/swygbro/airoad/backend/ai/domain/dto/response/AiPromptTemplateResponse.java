package com.swygbro.airoad.backend.ai.domain.dto.response;

import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.entity.AiPromptTemplate;
import com.swygbro.airoad.backend.ai.domain.entity.PromptType;

import lombok.Builder;

/**
 * AI 프롬프트 템플릿 조회 결과를 담는 DTO
 *
 * @param id 템플릿 ID
 * @param promptType 프롬프트 타입
 * @param agentType 에이전트 타입
 * @param prompt 프롬프트 내용
 * @param isActive 활성 상태
 * @param description 프롬프트 설명
 */
@Builder
public record AiPromptTemplateResponse(
    Long id,
    PromptType promptType,
    AgentType agentType,
    String prompt,
    Boolean isActive,
    String description) {

  /**
   * AiPromptTemplate 엔티티를 AiPromptTemplateResponse DTO로 변환합니다.
   *
   * @param template 변환할 엔티티
   * @return 변환된 DTO
   */
  public static AiPromptTemplateResponse of(AiPromptTemplate template) {
    return new AiPromptTemplateResponse(
        template.getId(),
        template.getPromptType(),
        template.getAgentType(),
        template.getPrompt(),
        template.getIsActive(),
        template.getDescription());
  }
}
