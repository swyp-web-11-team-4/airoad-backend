package com.swygbro.airoad.backend.ai.agent.trip.v2.optimizer;

import lombok.Getter;

@Getter
public enum TripPlanIssueType {
  CATEGORY_ORDER_VIOLATION("카테고리 순서 위반", "카테고리 순서가 정확하지 않음 (카테고리 순서를 지키지 않은 장소 명시)"),
  INVALID_SCHEDULE_COUNT("일정 개수 오류", "일정 개수가 요구사항과 일치하지 않음 (현재 개수와 필요한 개수를 명시)"),
  INVALID_RESTAURANT("식사 시간 장소 오류", "점심 또는 저녁 시간에 실제 음식점이 아닌 장소가 배치됨"),
  MOVEMENT_TIME_EXCEEDED("이동 시간 초과", "특정 장소의 이동 시간이 평가 기준 시간을 초과함 (어느 구간인지 명시)");
  private final String name;
  private final String description;

  TripPlanIssueType(String name, String description) {
    this.name = name;
    this.description = description;
  }
}
