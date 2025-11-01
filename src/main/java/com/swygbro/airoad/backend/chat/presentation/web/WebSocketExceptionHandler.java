package com.swygbro.airoad.backend.chat.presentation.web;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;

import com.swygbro.airoad.backend.common.domain.dto.ErrorResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.common.exception.WebSocketErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 전역 예외 처리 핸들러
 *
 * <p>WebSocket/STOMP 메시지 처리 중 발생하는 모든 예외를 일관된 형식으로 처리하여 클라이언트에 응답합니다.
 *
 * <h3>처리 방식</h3>
 *
 * <ul>
 *   <li>예외 발생 시 {@code /user/sub/errors/{chatRoomId}} 경로로 에러 메시지 전송
 *   <li>destination에서 chatRoomId 추출하여 해당 채팅방의 에러 채널로 전송
 *   <li>사용자별 개인 메시지로 전송 (다른 사용자에게 노출되지 않음)
 *   <li>모든 예외는 로그에 기록
 * </ul>
 *
 * <h3>클라이언트 구독 예시</h3>
 *
 * <pre>{@code
 * // JavaScript (STOMP.js)
 * stompClient.subscribe('/user/sub/errors/1', (message) => {
 *   const error = JSON.parse(message.body);
 *   console.error('WebSocket 에러:', error);
 *   showErrorNotification(error.message);
 * });
 * }</pre>
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class WebSocketExceptionHandler {

  private final SimpMessagingTemplate messagingTemplate;

  private static final Pattern SEND_DESTINATION_PATTERN =
      Pattern.compile("^/pub/chat/(\\d+)/message$");
  private static final Pattern SUBSCRIBE_DESTINATION_PATTERN =
      Pattern.compile("^/user/sub/chat/(\\d+)$");

  /**
   * BusinessException 처리
   *
   * <p>메시지 처리 중 발생한 비즈니스 예외를 WebSocket을 통해 사용자에게 전달합니다.
   *
   * @param e 발생한 BusinessException
   * @param principal 인증된 사용자 정보
   * @param headerAccessor STOMP 메시지 헤더 접근자
   */
  @MessageExceptionHandler(BusinessException.class)
  public void handleBusinessException(
      BusinessException e, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
    String userId = getUserId(principal);
    Long chatRoomId = extractChatRoomId(headerAccessor);

    log.warn(
        "[WebSocket] 비즈니스 예외 발생 - code: {}, message: {}, chatRoomId: {}",
        e.getErrorCode().getCode(),
        e.getMessage(),
        chatRoomId);

    ErrorResponse errorResponse =
        ErrorResponse.of(e.getErrorCode().getCode(), e.getMessage(), "/websocket");

    sendErrorToUser(userId, chatRoomId, errorResponse);
  }

  /**
   * 일반 예외 처리
   *
   * <p>메시지 처리 중 발생한 예상치 못한 예외를 WebSocket을 통해 사용자에게 전달합니다.
   *
   * @param e 발생한 Exception
   * @param principal 인증된 사용자 정보
   * @param headerAccessor STOMP 메시지 헤더 접근자
   */
  @MessageExceptionHandler(Exception.class)
  public void handleException(
      Exception e, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
    String userId = getUserId(principal);
    Long chatRoomId = extractChatRoomId(headerAccessor);

    log.error("[WebSocket] 예외 발생 - chatRoomId: {}", chatRoomId, e);

    ErrorResponse errorResponse =
        ErrorResponse.of(
            WebSocketErrorCode.INTERNAL_ERROR.getCode(),
            WebSocketErrorCode.INTERNAL_ERROR.getDefaultMessage(),
            "/websocket");

    sendErrorToUser(userId, chatRoomId, errorResponse);
  }

  /**
   * STOMP 메시지 destination에서 chatRoomId 추출
   *
   * <p>다음 패턴에서 chatRoomId를 추출합니다:
   *
   * <ul>
   *   <li>SEND: {@code /pub/chat/{chatRoomId}/message}
   *   <li>SUBSCRIBE: {@code /user/sub/chat/{chatRoomId}}
   * </ul>
   *
   * @param headerAccessor STOMP 메시지 헤더 접근자
   * @return chatRoomId (추출 실패 시 null)
   */
  private Long extractChatRoomId(SimpMessageHeaderAccessor headerAccessor) {
    if (headerAccessor == null) {
      return null;
    }

    String destination = headerAccessor.getDestination();
    if (destination == null) {
      return null;
    }

    // /pub/chat/{chatRoomId}/message 패턴에서 chatRoomId 추출
    Matcher sendMatcher = SEND_DESTINATION_PATTERN.matcher(destination);
    if (sendMatcher.matches()) {
      try {
        return Long.parseLong(sendMatcher.group(1));
      } catch (NumberFormatException e) {
        log.warn("[WebSocket] chatRoomId 파싱 실패 (SEND) - destination: {}", destination);
        return null;
      }
    }

    // /user/sub/chat/{chatRoomId} 패턴에서 chatRoomId 추출
    Matcher subscribeMatcher = SUBSCRIBE_DESTINATION_PATTERN.matcher(destination);
    if (subscribeMatcher.matches()) {
      try {
        return Long.parseLong(subscribeMatcher.group(1));
      } catch (NumberFormatException e) {
        log.warn("[WebSocket] chatRoomId 파싱 실패 (SUBSCRIBE) - destination: {}", destination);
        return null;
      }
    }

    return null;
  }

  /**
   * 사용자에게 에러 메시지 전송
   *
   * @param userId 사용자 ID
   * @param chatRoomId 채팅방 ID (null이면 기본 에러 채널 사용)
   * @param errorResponse 에러 응답 객체
   */
  private void sendErrorToUser(String userId, Long chatRoomId, ErrorResponse errorResponse) {
    // chatRoomId가 있으면 해당 채팅방의 에러 채널로 전송
    String destination = chatRoomId != null ? "/sub/errors/" + chatRoomId : "/sub/errors/unknown";

    messagingTemplate.convertAndSendToUser(userId, destination, errorResponse);

    log.debug(
        "[WebSocket] 에러 메시지 전송 - destination: {}, code: {}", destination, errorResponse.code());
  }

  /**
   * 사용자 ID 추출
   *
   * @param principal 인증된 사용자 정보
   * @return 사용자 ID (principal이 null이면 "unknown")
   */
  private String getUserId(Principal principal) {
    if (principal == null) {
      return "unknown";
    }
    return principal.getName();
  }
}
