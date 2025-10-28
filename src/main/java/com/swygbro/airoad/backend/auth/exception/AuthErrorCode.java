package com.swygbro.airoad.backend.auth.exception;

import org.springframework.http.HttpStatus;

import com.swygbro.airoad.backend.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
  // Member 관련 에러
  MEMBER_NOT_FOUND("MEMBER001", HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
  MEMBER_ALREADY_EXISTS("MEMBER002", HttpStatus.CONFLICT, "이미 존재하는 회원입니다."),
  INVALID_MEMBER_INFO("MEMBER003", HttpStatus.BAD_REQUEST, "올바르지 않은 회원 정보입니다."),

  // JWT 토큰 관련 에러
  INVALID_TOKEN("AUTH001", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
  EXPIRED_TOKEN("AUTH002", HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
  MALFORMED_TOKEN("AUTH003", HttpStatus.UNAUTHORIZED, "잘못된 형식의 토큰입니다."),
  UNSUPPORTED_TOKEN("AUTH004", HttpStatus.UNAUTHORIZED, "지원하지 않는 토큰입니다."),
  EMPTY_TOKEN("AUTH005", HttpStatus.UNAUTHORIZED, "토큰이 비어있습니다."),
  INVALID_TOKEN_SIGNATURE("AUTH006", HttpStatus.UNAUTHORIZED, "잘못된 토큰 서명입니다."),

  // OAuth2 관련 에러
  OAUTH2_AUTHORIZATION_REQUEST_NOT_FOUND(
      "AUTH007", HttpStatus.BAD_REQUEST, "OAuth2 인증 요청을 찾을 수 없습니다."),
  OAUTH2_COOKIE_DESERIALIZATION_FAILED(
      "AUTH008", HttpStatus.BAD_REQUEST, "OAuth2 쿠키 역직렬화에 실패했습니다."),
  OAUTH2_COOKIE_SERIALIZATION_FAILED(
      "AUTH009", HttpStatus.INTERNAL_SERVER_ERROR, "OAuth2 쿠키 직렬화에 실패했습니다."),
  COOKIE_SIZE_EXCEEDED("AUTH010", HttpStatus.BAD_REQUEST, "쿠키 크기가 최대 허용치를 초과했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String defaultMessage;
}
