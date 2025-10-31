package com.swygbro.airoad.backend.ai.exception;

import org.springframework.http.HttpStatus;

import com.swygbro.airoad.backend.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {
  AGENT_NOT_FOUND("AI001", HttpStatus.NOT_FOUND, "에이전트를 찾을 수 없습니다"),

  TRIP_PLAN_GENERATION_ERROR("AI002", HttpStatus.INTERNAL_SERVER_ERROR, "여행 일정 생성 중 오류가 발생했습니다"),

  AGENT_EXECUTION_FAILED("AI003", HttpStatus.INTERNAL_SERVER_ERROR, "AI 에이전트 실행 중 오류가 발생했습니다"),

  JSON_PARSING_FAILED("AI004", HttpStatus.INTERNAL_SERVER_ERROR, "JSON 파싱 중 오류가 발생했습니다");

  private final String code;
  private final HttpStatus httpStatus;
  private final String defaultMessage;
}
