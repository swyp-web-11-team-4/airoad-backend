package com.swygbro.airoad.backend.trip.application;

import com.swygbro.airoad.backend.trip.domain.dto.request.DailyPlanCreateRequest;

public interface DailyPlanCommandUseCase {

  /**
   * 일일 여행 계획을 데이터베이스에 저장합니다.
   *
   * @param chatRoomId 채팅방 ID (WebSocket 알림용)
   * @param tripPlanId 전체 여행 계획 ID
   * @param username 사용자 이름
   * @param dailyPlanCreateRequest 저장할 일일 계획 데이터
   */
  void saveDailyPlan(
      Long chatRoomId,
      Long tripPlanId,
      String username,
      DailyPlanCreateRequest dailyPlanCreateRequest);

  /**
   * 두 날 사이의 예약된 장소를 서로 교환합니다.
   *
   * @param chatRoomId 채팅방 ID (WebSocket 알림용)
   * @param tripPlanId 전체 여행 계획 ID
   * @param username 사용자 이름
   * @param dayNumberA 첫 번째 교환 대상 날의 번호
   * @param visitOrderA 첫 번째 교환 대상 날에서의 장소 방문 순서
   * @param dayNumberB 두 번째 교환 대상 날의 번호
   * @param visitOrderB 두 번째 교환 대상 날에서의 장소 방문 순서
   */
  void swapScheduledPlacesBetweenDays(
      Long chatRoomId,
      Long tripPlanId,
      String username,
      Integer dayNumberA,
      Integer visitOrderA,
      Integer dayNumberB,
      Integer visitOrderB);
}
