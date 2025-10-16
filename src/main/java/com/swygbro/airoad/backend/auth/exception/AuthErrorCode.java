package com.swygbro.airoad.backend.auth.exception;

import org.springframework.http.HttpStatus;

import com.swygbro.airoad.backend.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
  MEMBER_NOT_FOUND("MEMBER001", HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
  MEMBER_ALREADY_EXISTS("MEMBER002", HttpStatus.CONFLICT, "이미 존재하는 회원입니다."),
  INVALID_MEMBER_INFO("MEMBER003", HttpStatus.BAD_REQUEST, "올바르지 않은 회원 정보입니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String defaultMessage;
}
