package com.swygbro.airoad.backend.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket 통신 관련 에러 코드를 정의합니다.
 *
 * <p>WebSocket/STOMP 프로토콜 레벨의 에러와 실시간 메시징 도메인의 에러를 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum WebSocketErrorCode implements ErrorCode {
  // 연결 및 인증 관련 에러
  UNAUTHORIZED_CONNECTION("WS001", HttpStatus.UNAUTHORIZED, "WebSocket 연결 인증에 실패했습니다."),
  SESSION_EXPIRED("WS002", HttpStatus.UNAUTHORIZED, "WebSocket 세션이 만료되었습니다."),
  HANDSHAKE_FAILED("WS003", HttpStatus.BAD_REQUEST, "WebSocket 핸드셰이크에 실패했습니다."),

  // 메시지 처리 관련 에러
  INVALID_MESSAGE_FORMAT("WS101", HttpStatus.BAD_REQUEST, "메시지 형식이 올바르지 않습니다."),
  MESSAGE_TOO_LARGE("WS102", HttpStatus.PAYLOAD_TOO_LARGE, "메시지 크기가 제한을 초과했습니다."),
  INVALID_DESTINATION("WS103", HttpStatus.BAD_REQUEST, "유효하지 않은 메시지 destination입니다."),

  // 채팅방 관련 에러
  CHAT_ROOM_NOT_FOUND("WS201", HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다."),
  CHAT_ROOM_ACCESS_DENIED("WS202", HttpStatus.FORBIDDEN, "해당 채팅방에 접근 권한이 없습니다."),

  // 전송 및 세션 관련 에러
  MESSAGE_DELIVERY_FAILED("WS301", HttpStatus.INTERNAL_SERVER_ERROR, "메시지 전송에 실패했습니다."),
  SEND_BUFFER_OVERFLOW("WS302", HttpStatus.SERVICE_UNAVAILABLE, "전송 버퍼가 가득 찼습니다."),
  SEND_TIMEOUT("WS303", HttpStatus.REQUEST_TIMEOUT, "메시지 전송 시간이 초과되었습니다."),
  CONNECTION_LIMIT_EXCEEDED("WS304", HttpStatus.TOO_MANY_REQUESTS, "동시 연결 수가 제한을 초과했습니다."),

  // 서비스 레이어 에러
  AI_SERVICE_UNAVAILABLE("WS401", HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스를 사용할 수 없습니다."),
  MESSAGE_SAVE_FAILED("WS402", HttpStatus.INTERNAL_SERVER_ERROR, "메시지 저장에 실패했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String defaultMessage;
}
