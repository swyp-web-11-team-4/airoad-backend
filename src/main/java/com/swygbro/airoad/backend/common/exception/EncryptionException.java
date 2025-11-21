package com.swygbro.airoad.backend.common.exception;

/** 암호화 처리 중 발생하는 예외 */
public class EncryptionException extends RuntimeException {

  public EncryptionException(String message) {
    super(message);
  }

  public EncryptionException(String message, Throwable cause) {
    super(message, cause);
  }
}
