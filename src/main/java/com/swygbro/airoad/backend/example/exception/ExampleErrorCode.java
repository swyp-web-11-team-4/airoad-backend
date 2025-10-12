package com.swygbro.airoad.backend.example.exception;

import org.springframework.http.HttpStatus;

import com.swygbro.airoad.backend.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExampleErrorCode implements ErrorCode {
  EXAMPLE_NOT_FOUND("EXAMPLE001", HttpStatus.NOT_FOUND, "Example을 찾을 수 없습니다."),
  EXAMPLE_ALREADY_EXISTS("EXAMPLE002", HttpStatus.CONFLICT, "이미 존재하는 Example입니다."),
  INVALID_EXAMPLE_NAME("EXAMPLE003", HttpStatus.BAD_REQUEST, "유효하지 않은 Example 이름입니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String defaultMessage;
}
