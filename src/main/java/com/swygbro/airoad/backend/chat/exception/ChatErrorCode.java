package com.swygbro.airoad.backend.chat.exception;

import org.springframework.http.HttpStatus;

import com.swygbro.airoad.backend.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 채팅 도메인 비즈니스 로직 에러 코드를 정의합니다.
 *
 * <p>채팅방, 메시지 처리, REST API 조회 등 비즈니스 로직에서 발생하는 에러를 정의합니다. WebSocket 프로토콜/전송 레벨 에러는 {@link
 * com.swygbro.airoad.backend.common.exception.WebSocketErrorCode}를 사용합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {
  // 메시지 처리 관련
  MESSAGE_NOT_FOUND("CHAT101", HttpStatus.NOT_FOUND, "존재하지 않는 메시지입니다."),
  INVALID_MESSAGE_FORMAT("CHAT102", HttpStatus.BAD_REQUEST, "메시지 형식이 올바르지 않습니다."),
  MESSAGE_TOO_LARGE("CHAT103", HttpStatus.PAYLOAD_TOO_LARGE, "메시지 크기가 제한을 초과했습니다."),

  // 페이징 관련
  INVALID_CURSOR("CHAT201", HttpStatus.BAD_REQUEST, "유효하지 않은 커서입니다."),
  INVALID_PAGE_SIZE("CHAT202", HttpStatus.BAD_REQUEST, "페이지 크기는 1 이상 100 이하여야 합니다."),

  // 채팅방 관련
  CONVERSATION_NOT_FOUND("CHAT301", HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다."),
  CONVERSATION_ACCESS_DENIED("CHAT302", HttpStatus.FORBIDDEN, "해당 채팅방에 접근 권한이 없습니다."),
  CONVERSATION_ALREADY_EXISTS("CHAT303", HttpStatus.CONFLICT, "이미 존재하는 대화방입니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String defaultMessage;
}
