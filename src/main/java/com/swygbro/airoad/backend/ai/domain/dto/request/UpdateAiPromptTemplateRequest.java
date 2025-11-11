package com.swygbro.airoad.backend.ai.domain.dto.request;

import lombok.Builder;

/**
 * AI 프롬프트 템플릿 부분 수정을 요청하는 DTO
 *
 * @param prompt 수정할 프롬프트 내용
 * @param isActive 변경할 활성 상태
 * @param description 수정할 설명
 */
@Builder
public record UpdateAiPromptTemplateRequest(String prompt, Boolean isActive, String description) {}
