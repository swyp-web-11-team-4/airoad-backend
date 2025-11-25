package com.swygbro.airoad.backend.common.exception;

/** 복호화 처리 중 발생하는 예외 */
public class DecryptionException extends RuntimeException {

  public DecryptionException(String message) {
    super(message);
  }

  public DecryptionException(String message, Throwable cause) {
    super(message, cause);
  }
}
