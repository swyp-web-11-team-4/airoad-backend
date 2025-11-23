package com.swygbro.airoad.backend.ai.agent.trip.v2.worker;

import lombok.Getter;

/** TripPlan 워커 작업 타입 */
@Getter
public enum TripPlanTaskType {
  KEYWORD("keyword", "검색 키워드 생성"),
  VECTOR_SEARCH("vector_search", "벡터 검색으로 장소 탐색 및 중복 제거"),
  SCHEDULE("schedule", "최종 일정 생성"),
  MODIFY("modify", "기존 일정 부분 수정 및 보완"),
  DISTANCE_CALC("distance_calc", "거리 계산");

  private final String type;
  private final String description;

  TripPlanTaskType(String type, String description) {
    this.type = type;
    this.description = description;
  }
}
