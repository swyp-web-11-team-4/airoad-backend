package com.swygbro.airoad.backend.trip.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 여행 일정 생성 진행 상황을 WebSocket을 통해 클라이언트로 전송하는 메시지 DTO입니다.
 *
 * <p>일정 생성 과정의 각 단계(일차별 완료, 전체 완료, 오류 등)를 실시간으로 전달합니다.
 *
 * @param type 메시지 타입 (DAILY_PLAN_GENERATED, COMPLETED, ERROR, CANCELLED)
 * @param tripPlanId 여행 일정 ID
 * @param dailyPlan 일차별 일정 데이터 (type이 DAILY_PLAN_GENERATED인 경우)
 * @param message 상태 메시지 (완료 메시지, 오류 메시지 등)
 * @param errorCode 오류 코드 (type이 ERROR인 경우)
 */
@Builder
@Schema(description = "여행 일정 생성 진행 상황 메시지")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TripPlanProgressMessage(
    @Schema(description = "메시지 타입", example = "DAILY_PLAN_GENERATED") MessageType type,
    @Schema(description = "여행 일정 ID", example = "1") Long tripPlanId,
    @Schema(description = "일차별 일정 데이터") DailyPlanResponse dailyPlan,
    @Schema(description = "상태 메시지", example = "1일차 일정이 생성되었습니다.") String message,
    @Schema(description = "오류 코드", example = "TRIP101") String errorCode) {

  /** 메시지 타입 enum */
  public enum MessageType {

    /** 일차별 일정 생성 완료 */
    DAILY_PLAN_GENERATED,
    /** 전체 일정 생성 완료 */
    COMPLETED,
    /** 일정 수정 시작 */
    UPDATE_STARTED,
    /** 일정 수정 완료 */
    UPDATED,
    /** 오류 발생 */
    ERROR,
    /** 생성 취소됨 */
    CANCELLED
  }
}
