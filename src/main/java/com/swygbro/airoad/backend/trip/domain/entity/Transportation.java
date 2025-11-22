package com.swygbro.airoad.backend.trip.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 이동 수단을 정의하는 Enum */
@Getter
@RequiredArgsConstructor
public enum Transportation {
  NONE("이동 수단 없음"),
  WALKING("도보"),
  PUBLIC_TRANSIT("대중교통"),
  CAR("자동차");

  private final String description;
}
