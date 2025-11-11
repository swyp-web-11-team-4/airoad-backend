package com.swygbro.airoad.backend.ai.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiPromptTemplate extends BaseEntity {

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private PromptType promptType;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private AgentType agentType;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String prompt;

  @Column private Boolean isActive;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Builder
  private AiPromptTemplate(
      PromptType promptType,
      AgentType agentType,
      String prompt,
      Boolean isActive,
      String description) {
    this.promptType = promptType;
    this.agentType = agentType;
    this.prompt = prompt;
    this.isActive = isActive != null ? isActive : false;
    this.description = description;
  }

  /**
   * 프롬프트를 업데이트하는 메서드.
   *
   * @param prompt 새롭게 업데이트할 프롬프트
   */
  public void updatePrompt(String prompt) {
    this.prompt = prompt;
  }

  /**
   * 프롬프트 활성 상태를 업데이트하는 메서드.
   *
   * @param isActive 업데이트할 활성 상태 (true: 활성화, false: 비활성화)
   */
  public void updatePromptStatus(Boolean isActive) {
    this.isActive = isActive;
  }

  /**
   * 프롬프트 설명을 업데이트하는 메서드.
   *
   * @param description 업데이트할 설명 내용
   */
  public void updateDescription(String description) {
    this.description = description;
  }
}
