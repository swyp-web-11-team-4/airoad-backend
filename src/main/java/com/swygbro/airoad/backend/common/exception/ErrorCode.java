package com.swygbro.airoad.backend.common.exception;

import java.io.Serializable;

import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 에러 코드의 공통 인터페이스입니다.
 *
 * <p>각 도메인에서 이 인터페이스를 구현하여 도메인별 에러 코드를 정의할 수 있습니다.
 */
public interface ErrorCode extends Serializable {

  /**
   * 애플리케이션 레벨의 에러 코드를 반환합니다.
   *
   * @return 에러 코드 (예: "USER001", "COMMON001")
   */
  String getCode();

  /**
   * HTTP 상태 코드를 반환합니다.
   *
   * @return HTTP 상태 코드
   */
  HttpStatus getHttpStatus();

  /**
   * 기본 에러 메시지를 반환합니다.
   *
   * @return 기본 에러 메시지
   */
  String getDefaultMessage();
}
