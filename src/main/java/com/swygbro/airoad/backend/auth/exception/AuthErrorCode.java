package com.swygbro.airoad.backend.auth.exception;

import org.springframework.http.HttpStatus;

import com.swygbro.airoad.backend.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
  // 인증 관련 에러
  AUTHENTICATION_REQUIRED("AUTH011", HttpStatus.UNAUTHORIZED, "인증이 필요합니다. 로그인해주세요."),

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
