package com.swygbro.airoad.backend.trip.application;

import java.util.List;

import com.swygbro.airoad.backend.trip.domain.dto.request.DailyPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;

public interface DailyPlanUseCase {

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
   * @param tripPlanId 전체 여행 계획 ID
   */
  List<DailyPlanResponse> getDailyPlanListByTripPlanId(Long tripPlanId, Long memberId);
}
