package com.swygbro.airoad.backend.content.domain.entity;

import jakarta.persistence.*;

import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 테마에 해당하는 장소를 저장하는 엔티티 */
@Entity
@Table(name = "place_theme")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceTheme extends BaseEntity {

  /** 테마와 연관된 장소 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Place place;

  /** 여행 테마명 (힐링, 맛집, 액티비티 등) */
  @Column(nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private PlaceThemeType placeThemeType;

  @Builder
  private PlaceTheme(Place place, PlaceThemeType placeThemeType) {
    this.place = place;
    this.placeThemeType = placeThemeType;
  }
}
