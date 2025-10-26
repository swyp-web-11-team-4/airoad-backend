package com.swygbro.airoad.backend.chat.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** AI와 사용자 간의 대화 세션을 나타내는 엔티티 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiConversation extends BaseEntity {

  /** 대화의 주체인 사용자 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Member member;

  /** 대화와 관련된 여행 계획 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private TripPlan tripPlan;

  @Builder
  private AiConversation(Member member, TripPlan tripPlan) {
    this.member = member;
    this.tripPlan = tripPlan;
  }

  /**
   * 주어진 이메일이 이 대화방의 소유자인지 확인합니다.
   *
   * @param email 확인할 사용자 이메일
   * @return 소유자인 경우 true, 아니면 false
   * @throws IllegalArgumentException email이 null인 경우
   */
  public boolean isOwner(String email) {
    if (email == null) {
      throw new IllegalArgumentException("email은 null일 수 없습니다");
    }
    return this.member.getEmail().equals(email);
  }

  /**
   * 대화와 관련된 여행 계획 ID를 반환합니다.
   *
   * @return 여행 계획 ID
   */
  public Long getTripPlanId() {
    return this.tripPlan.getId();
  }
}
