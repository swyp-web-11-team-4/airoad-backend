package com.swygbro.airoad.backend.trip.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 일일 계획의 장소 방문 일정을 분류하는 Enum */
@Getter
@RequiredArgsConstructor
public enum ScheduledCategory {
  MORNING("오전 일정"),
  LUNCH("점심 식사"),
  AFTERNOON("오후 일정"),
  DINNER("저녁 식사"),
  EVENING("저녁 일정");

  private final String description;
}
