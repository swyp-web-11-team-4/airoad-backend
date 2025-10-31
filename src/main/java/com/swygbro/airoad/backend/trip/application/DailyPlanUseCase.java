package com.swygbro.airoad.backend.trip.application;

import com.swygbro.airoad.backend.trip.domain.dto.request.DailyPlanCreateRequest;

public interface DailyPlanUseCase {

  /**
   * 일일 여행 계획을 데이터베이스에 저장합니다.
   *
   * @param chatRoomId 채팅방 ID (WebSocket 알림용)
   * @param tripPlanId 전체 여행 계획 ID
   * @param dailyPlanCreateRequest 저장할 일일 계획 데이터
   */
  void saveDailyPlan(
      Long chatRoomId, Long tripPlanId, DailyPlanCreateRequest dailyPlanCreateRequest);
}
