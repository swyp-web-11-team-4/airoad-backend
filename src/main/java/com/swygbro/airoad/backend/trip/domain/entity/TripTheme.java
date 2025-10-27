package com.swygbro.airoad.backend.trip.domain.entity;

import jakarta.persistence.*;

import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 여행 계획과 테마의 매핑을 나타내는 엔티티 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripTheme extends BaseEntity {

  /** 여행 계획 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private TripPlan tripPlan;

  /** 테마 장소 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private PlaceTheme placeTheme;

  /** 테마 우선순위 */
  @Column private Integer priority;

  @Builder
  private TripTheme(TripPlan tripPlan, PlaceTheme placeTheme, Integer priority) {
    this.tripPlan = tripPlan;
    this.placeTheme = placeTheme;
    this.priority = priority;
  }
}
