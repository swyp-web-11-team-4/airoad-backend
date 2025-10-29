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
  TRIP_PLAN_NOT_FOUND("TRIP001", HttpStatus.NOT_FOUND, "여행 일정을 찾을 수 없습니다."),
  TRIP_PLAN_ALREADY_COMPLETED("TRIP002", HttpStatus.CONFLICT, "이미 완료된 여행 일정입니다."),
  INVALID_DATE_RANGE("TRIP003", HttpStatus.BAD_REQUEST, "올바르지 않은 날짜 범위입니다."),

  // AI 일정 생성 관련 에러
  TRIP_GENERATION_FAILED("TRIP101", HttpStatus.INTERNAL_SERVER_ERROR, "여행 일정 생성에 실패했습니다."),
  STREAM_TIMEOUT("TRIP102", HttpStatus.REQUEST_TIMEOUT, "AI 응답 스트림이 시간 초과되었습니다."),
  PARSING_ERROR("TRIP103", HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답 파싱 중 오류가 발생했습니다."),
  PLACE_SEARCH_FAILED("TRIP104", HttpStatus.INTERNAL_SERVER_ERROR, "조건에 맞는 관광지를 찾지 못했습니다."),
  LLM_CONNECTION_ERROR("TRIP105", HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스 연결에 실패했습니다."),
  INVALID_GENERATION_REQUEST("TRIP106", HttpStatus.BAD_REQUEST, "올바르지 않은 일정 생성 요청입니다."),

  // 일정 수정 관련 에러
  DAILY_PLAN_NOT_FOUND("TRIP201", HttpStatus.NOT_FOUND, "일차별 일정을 찾을 수 없습니다."),
  SCHEDULED_PLACE_NOT_FOUND("TRIP202", HttpStatus.NOT_FOUND, "예정된 방문 장소를 찾을 수 없습니다."),
  INVALID_VISIT_ORDER("TRIP203", HttpStatus.BAD_REQUEST, "올바르지 않은 방문 순서입니다."),

  // 권한 관련 에러
  UNAUTHORIZED_TRIP_ACCESS("TRIP301", HttpStatus.FORBIDDEN, "여행 일정에 접근할 권한이 없습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String defaultMessage;
}
