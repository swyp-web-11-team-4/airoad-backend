package com.swygbro.airoad.backend.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket 통신 관련 에러 코드를 정의합니다.
 *
 * <p>WebSocket/STOMP 프로토콜 레벨의 연결, 전송, 세션 관리 에러를 정의합니다. 채팅 비즈니스 로직 에러는 {@link
 * com.swygbro.airoad.backend.chat.exception.ChatErrorCode}를 사용합니다.
 */
@Getter
@RequiredArgsConstructor
public enum WebSocketErrorCode implements ErrorCode {
  // 연결 및 인증 관련 에러
  UNAUTHORIZED_CONNECTION("WS001", HttpStatus.UNAUTHORIZED, "WebSocket 연결 인증에 실패했습니다."),
  SESSION_EXPIRED("WS002", HttpStatus.UNAUTHORIZED, "WebSocket 세션이 만료되었습니다."),
  HANDSHAKE_FAILED("WS003", HttpStatus.BAD_REQUEST, "WebSocket 핸드셰이크에 실패했습니다."),
  FORBIDDEN_SUBSCRIPTION("WS004", HttpStatus.FORBIDDEN, "구독 권한이 없습니다."),
  FORBIDDEN_SEND("WS005", HttpStatus.FORBIDDEN, "메시지 전송 권한이 없습니다."),

  // 프로토콜 레벨 에러
  INVALID_DESTINATION("WS101", HttpStatus.BAD_REQUEST, "유효하지 않은 메시지 destination입니다."),
  INVALID_FRAME("WS102", HttpStatus.BAD_REQUEST, "유효하지 않은 WebSocket 프레임입니다."),

  // 전송 및 세션 관련 에러
  MESSAGE_DELIVERY_FAILED("WS201", HttpStatus.INTERNAL_SERVER_ERROR, "메시지 전송에 실패했습니다."),
  SEND_BUFFER_OVERFLOW("WS202", HttpStatus.SERVICE_UNAVAILABLE, "전송 버퍼가 가득 찼습니다."),
  SEND_TIMEOUT("WS203", HttpStatus.REQUEST_TIMEOUT, "메시지 전송 시간이 초과되었습니다."),
  CONNECTION_LIMIT_EXCEEDED("WS204", HttpStatus.TOO_MANY_REQUESTS, "동시 연결 수가 제한을 초과했습니다."),

  // 서비스 레이어 에러
  AI_SERVICE_UNAVAILABLE("WS301", HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스를 사용할 수 없습니다."),
  MESSAGE_SAVE_FAILED("WS302", HttpStatus.INTERNAL_SERVER_ERROR, "메시지 저장에 실패했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String defaultMessage;
}
