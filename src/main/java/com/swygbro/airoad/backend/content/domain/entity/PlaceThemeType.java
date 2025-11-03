package com.swygbro.airoad.backend.content.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 장소 테마 (한 장소는 여러 테마를 가질 수 있음) */
@Getter
@RequiredArgsConstructor
public enum PlaceThemeType {
  FAMOUS_SPOT("유명 관광지"),
  HEALING("힐링"),
  SNS_HOTSPOT("sns 핫플"),
  EXPERIENCE_ACTIVITY("체험 액티비티"),
  CULTURE_ART("문화/예술"),
  SHOPPING("쇼핑"),
  RESTAURANT("음식점");

  private final String description;
}
