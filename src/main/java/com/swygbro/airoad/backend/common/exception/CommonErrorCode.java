package com.swygbro.airoad.backend.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공통 에러 코드를 정의합니다.
 *
 * <p>애플리케이션 전반에서 사용되는 공통적인 에러를 정의합니다. 도메인별 특화된 에러는 각 도메인의 ErrorCode enum에서 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
  INVALID_INPUT("COMMON001", HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
  INVALID_TYPE("COMMON002", HttpStatus.BAD_REQUEST, "잘못된 데이터 타입입니다."),
  MISSING_PARAMETER("COMMON003", HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다."),

  UNAUTHORIZED("COMMON401", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
  FORBIDDEN("COMMON403", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
  RESOURCE_NOT_FOUND("COMMON404", HttpStatus.NOT_FOUND, "해당하는 리소스를 찾을 수 없습니다."),
  INTERNAL_ERROR("COMMON500", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String defaultMessage;
}
