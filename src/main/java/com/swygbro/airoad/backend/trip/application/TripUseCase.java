package com.swygbro.airoad.backend.trip.application;

import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.ChannelIdResponse;

/** 여행 계획 관련 유스케이스를 정의하는 인터페이스입니다. */
public interface TripUseCase {

  /**
   * 여행 일정 생성 세션을 시작합니다.
   *
   * <p>사용자가 사용할 채팅방과 여행계획을 생성하고 그 ID를 반환합니다.
   *
   * @param username 사용자 이름 (이메일)
   * @param request 여행 계획 요청 dto
   * @return 채팅방 ID와 여행 계획 ID
   */
  ChannelIdResponse createTripPlanSession(String username, TripPlanCreateRequest request);

  /**
   * 여행 일정 생성을 요청합니다.
   *
   * <p>사용자가 입력한 여행 조건을 바탕으로 AI 기반 여행 일정 생성 이벤트를 발행합니다.
   *
   * @param username 사용자 이름 (이메일)
   * @param chatRoomId 채팅방 ID
   */
  void startTripPlanGeneration(String username, Long chatRoomId);
}
