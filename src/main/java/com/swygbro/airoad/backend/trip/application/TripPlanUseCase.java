package com.swygbro.airoad.backend.trip.application;

import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanUpdateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.ChannelIdResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanResponse;

/**
 * 여행 일정 관리 Use Case 인터페이스
 *
 * <p>여행 일정 조회 및 삭제 등의 비즈니스 로직을 정의합니다.
 */
public interface TripPlanUseCase {

  /**
   * 사용자의 여행 일정 목록을 커서 기반 페이지네이션으로 조회합니다.
   *
   * @param memberId 사용자 ID
   * @param size 조회할 일정 개수
   * @param cursor 페이징 커서 (이전 페이지의 마지막 tripPlanId, null이면 첫 페이지)
   * @param sort 정렬 기준 (형식: field:direction, 예: createdAt:desc)
   * @return 커서 페이지 응답
   */
  CursorPageResponse<TripPlanResponse> getUserTripPlans(
      Long memberId, int size, Long cursor, String sort);

  /**
   * 여행 일정을 삭제합니다. 소유자만 삭제할 수 있습니다.
   *
   * @param tripPlanId 삭제할 여행 일정 ID
   * @param memberId 요청한 사용자 ID
   * @throws BusinessException TRIP_PLAN_NOT_FOUND - 여행 일정을 찾을 수 없음
   * @throws BusinessException TRIP_PLAN_FORBIDDEN - 소유자가 아님
   */
  void deleteTripPlan(Long tripPlanId, Long memberId);

  /**
   * 여행 일정을 수정합니다.
   *
   * @param tripPlanId 수정할 여행 일정 ID
   * @param memberId 요청한 사용자 ID
   * @param request 수정할 여행 일정 정보
   */
  void updateTripPlan(Long tripPlanId, Long memberId, TripPlanUpdateRequest request);

  /**
   * 여행 일정 생성 세션을 시작합니다.
   *
   * <p>사용자가 사용할 채팅방과 여행계획을 생성하고 그 id를 반환합니다.
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
   * @param tripPlanId 여행 계획 Id
   */
  void startTripPlanGeneration(String username, Long tripPlanId);
}
