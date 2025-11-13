package com.swygbro.airoad.backend.trip.exception;

import org.springframework.http.HttpStatus;

import com.swygbro.airoad.backend.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 여행 도메인의 에러 코드를 정의합니다.
 *
 * <p>여행 일정 생성, 조회, 수정 등의 과정에서 발생할 수 있는 다양한 에러를 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum TripErrorCode implements ErrorCode {
  // 여행 일정 조회 관련 에러
  TRIP_PLAN_NOT_FOUND("TRIP101", HttpStatus.NOT_FOUND, "여행 일정을 찾을 수 없습니다."),
  TRIP_PLAN_FORBIDDEN("TRIP102", HttpStatus.FORBIDDEN, "여행 일정에 대한 접근 권한이 없습니다."),

  // 일일 일정 관련 에러
  DAILY_PLAN_NOT_FOUND("TRIP103", HttpStatus.NOT_FOUND, "일일 일정을 찾을 수 없습니다."),

  // 방문 장소 관련 에러
  SCHEDULED_PLACE_NOT_FOUND("TRIP104", HttpStatus.NOT_FOUND, "계획 방문 일정을 찾을 수 없습니다."),

  // 장소 관련 에러
  PLACE_NOT_FOUND("TRIP201", HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다");

  private final String code;
  private final HttpStatus httpStatus;
  private final String defaultMessage;
}
