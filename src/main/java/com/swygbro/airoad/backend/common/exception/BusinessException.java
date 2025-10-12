package com.swygbro.airoad.backend.common.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 처리 중 발생하는 예외를 나타냅니다.
 *
 * <p>ErrorCode를 통해 구조화된 에러 정보를 제공합니다.
 */
@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  /**
   * ErrorCode의 기본 메시지를 사용하여 예외를 생성합니다.
   *
   * @param errorCode 에러 코드
   */
  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getDefaultMessage());
    this.errorCode = errorCode;
  }

  /**
   * 커스텀 메시지를 사용하여 예외를 생성합니다.
   *
   * @param errorCode 에러 코드
   * @param message 커스텀 에러 메시지
   */
  public BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  /**
   * 커스텀 메시지와 원인 예외를 사용하여 예외를 생성합니다.
   *
   * @param errorCode 에러 코드
   * @param message 커스텀 에러 메시지
   * @param cause 원인 예외
   */
  public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }
}
