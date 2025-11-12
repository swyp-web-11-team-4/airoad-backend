package com.swygbro.airoad.backend.ai.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.entity.AiPromptTemplate;
import com.swygbro.airoad.backend.ai.domain.entity.PromptType;

public interface AiPromptTemplateRepository extends JpaRepository<AiPromptTemplate, Long> {
  /**
   * 특정 프롬프트 타입, 에이전트 타입으로 활성화된 AI 프롬프트 템플릿을 조회합니다.
   *
   * @param promptType 프롬프트 타입
   * @param agentType 에이전트 타입
   * @return 활성화된 AI 프롬프트 템플릿 목록
   */
  @Query(
      "SELECT t FROM AiPromptTemplate t WHERE t.promptType = :promptType AND t.agentType = :agentType AND t.isActive")
  Optional<AiPromptTemplate> findByActivePrompt(
      @Param("promptType") PromptType promptType, @Param("agentType") AgentType agentType);
}
