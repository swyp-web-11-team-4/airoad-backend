package com.swygbro.airoad.backend.ai.domain.dto.request;

import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.entity.PromptType;

import lombok.Builder;

/**
 * AI 프롬프트 템플릿 생성을 요청하는 DTO
 *
 * @param promptType 프롬프트 타입
 * @param agentType 에이전트 타입
 * @param prompt 프롬프트 내용
 * @param description 프롬프트 설명
 */
@Builder
public record CreateAiPromptTemplateRequest(
    PromptType promptType, AgentType agentType, String prompt, String description) {}
