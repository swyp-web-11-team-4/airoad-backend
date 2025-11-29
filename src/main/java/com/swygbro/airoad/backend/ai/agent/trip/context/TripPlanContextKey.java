package com.swygbro.airoad.backend.ai.agent.trip.context;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 여행 일정 생성 파이프라인에서 사용되는 ExecutionContext의 상태 키 열거형
 *
 * <p>각 키는 파이프라인의 각 단계에서 필요로 하는 데이터를 명시적으로 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum TripPlanContextKey {
  REQUEST("request", "여행 일정 생성 요청"),
  DAY_NUMBER("dayNumber", "현재 생성 중인 날짜 번호"),
  KEYWORDS("keywords", "생성된 검색 키워드"),
  SEARCH_RESULTS("searchResults", "벡터 검색 결과"),
  DEDUP_RESULT("dedupResult", "중복 제거 결과"),
  ROUTE_OPTIMIZATION_RESULT("routeOptimizationResult", "경로 최적화 결과"),
  SCHEDULE_SUMMARY_RESULT("scheduleSummaryResult", "일정 요약 결과"),
  DISTANCE_CALCULATED_PLACES("distanceCalculatedPlaces", "거리 계산 완료된 장소 목록"),
  SCHEDULE("schedule", "생성된 여행 일정"),
  PREVIOUS_PLACE_IDS("previousPlaceIds", "이전 날짜 방문 장소 ID 목록"),
  VISITED_DISTRICTS("visitedDistricts", "이전 날짜 방문 구역 목록"),
  SELECTED_DISTRICT("selectedDistrict", "현재 날짜 선택된 구역"),
  SELECTED_PROVINCE("selectedProvince", "현재 날짜 선택된 광역시/도");

  private final String key;
  private final String description;
}
