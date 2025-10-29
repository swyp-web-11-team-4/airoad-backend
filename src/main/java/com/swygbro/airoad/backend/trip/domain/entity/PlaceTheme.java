package com.swygbro.airoad.backend.trip.domain.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;
import com.swygbro.airoad.backend.content.domain.entity.Place;

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

  /** 테마와 연관된 대표 장소 목록 */
  @OneToMany(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private List<Place> places = new ArrayList<>();

  /** 여행 테마명 (힐링, 맛집, 액티비티 등) */
  @Column(unique = true, nullable = false, length = 50)
  private String themeName;

  @Builder
  private PlaceTheme(List<Place> places, String themeName) {
    this.places = places;
    this.themeName = themeName;
  }
}
