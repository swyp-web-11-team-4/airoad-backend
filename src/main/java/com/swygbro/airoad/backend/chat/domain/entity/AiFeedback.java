package com.swygbro.airoad.backend.chat.domain.entity;

import jakarta.persistence.*;

import com.swygbro.airoad.backend.common.domain.embeddable.Feedback;
import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** AI 메시지에 대한 사용자의 피드백을 나타내는 엔티티 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiFeedback extends BaseEntity {

  /** 피드백의 대상이 되는 AI 메시지 */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private AiMessage message;

  /** 사용자 피드백 정보 (점수, 내용) */
  @Embedded private Feedback feedback;

  /** 피드백 생성 시점의 시스템 프롬프트 */
  @Column(columnDefinition = "TEXT")
  private String systemPrompt;

  @Builder
  private AiFeedback(AiMessage message, Feedback feedback, String systemPrompt) {
    this.message = message;
    this.feedback = feedback;
    this.systemPrompt = systemPrompt;
  }
}
