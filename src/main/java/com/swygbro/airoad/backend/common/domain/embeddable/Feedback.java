package com.swygbro.airoad.backend.common.domain.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** AI 응답에 대한 사용자 피드백 정보를 나타내는 Embeddable 객체 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback {

  /** 사용자 피드백 점수 (1~5) */
  @Column(nullable = false)
  private Integer score;

  /** 사용자 피드백 내용 */
  @Lob
  @Column(columnDefinition = "TEXT")
  private String comment;

  @Builder
  public Feedback(Integer score, String comment) {
    this.score = score;
    this.comment = comment;
  }
}
