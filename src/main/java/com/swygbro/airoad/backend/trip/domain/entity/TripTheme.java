package com.swygbro.airoad.backend.trip.domain.entity;

import jakarta.persistence.*;

import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;
import com.swygbro.airoad.backend.content.domain.entity.Place;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 여행 테마를 나타내는 엔티티 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripTheme extends BaseEntity {

  /** 테마와 연관된 대표 장소 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Place place;

  /** 여행 테마명 (힐링, 맛집, 액티비티 등) */
  @Column(unique = true, nullable = false, length = 50)
  private String themeName;

  @Builder
  private TripTheme(Place place, String themeName) {
    this.place = place;
    this.themeName = themeName;
  }
}
