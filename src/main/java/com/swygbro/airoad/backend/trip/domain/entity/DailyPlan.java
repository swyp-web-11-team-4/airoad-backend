package com.swygbro.airoad.backend.trip.domain.entity;

import java.time.LocalDate;

import jakarta.persistence.*;

import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 여행의 일일 계획을 나타내는 엔티티 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyPlan extends BaseEntity {

  /** 일정이 속한 전체 여행 계획 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private TripPlan tripPlan;

  /** 일정 날짜 */
  @Column(nullable = false)
  private LocalDate date;

  @Builder
  private DailyPlan(TripPlan tripPlan, LocalDate date) {
    this.tripPlan = tripPlan;
    this.date = date;
  }
}
