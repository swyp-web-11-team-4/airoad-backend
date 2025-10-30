package com.swygbro.airoad.backend.ai.exception;

import com.swygbro.airoad.backend.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {
  AGENT_NOT_FOUND("AI001", HttpStatus.NOT_FOUND, "에이전트를 찾을 수 없습니다"),

  TRIP_PLAN_GENERATION_ERROR("AI002", HttpStatus.INTERNAL_SERVER_ERROR, "여행 일정 생성 중 오류가 발생했습니다");

  private final String code;
  private final HttpStatus httpStatus;
  private final String defaultMessage;
}
