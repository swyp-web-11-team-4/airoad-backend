package com.swygbro.airoad.backend.chat.config;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.swygbro.airoad.backend.auth.application.UserDetailsServiceImpl;
import com.swygbro.airoad.backend.auth.domain.entity.TokenType;
import com.swygbro.airoad.backend.auth.filter.JwtTokenProvider;
import com.swygbro.airoad.backend.common.domain.dto.ErrorResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.common.exception.WebSocketErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket STOMP 메시지 인증 및 권한 검증 인터셉터
 *
 * <p>STOMP CONNECT, SUBSCRIBE, SEND 명령에 대해 다음과 같이 검증합니다:
 *
 * <h3>CONNECT</h3>
 *
 * <ul>
 *   <li>JWT 토큰 검증 (Authorization 헤더, 대소문자 무관)
 *   <li>Principal 설정
 * </ul>
 *
 * <h3>SUBSCRIBE</h3>
 *
 * <ul>
 *   <li>Principal null 체크
 *   <li>구독 경로 검증 (/user/sub/chat/*, /user/sub/schedule/*, /user/sub/errors/*만 허용)
 * </ul>
 *
 * <h3>SEND</h3>
 *
 * <ul>
 *   <li>Principal null 체크
 *   <li>전송 경로 검증 (/pub/chat/{chatRoomId}/message만 허용)
 * </ul>
 *
 * <h3>예외 처리</h3>
 *
 * <p><strong>중요</strong>: {@link ChannelInterceptor}에서 발생한 예외는 {@code @MessageExceptionHandler}에
 * 포착되지 않습니다. 인터셉터는 메시지가 컨트롤러에 도달하기 <strong>전</strong>에 실행되기 때문입니다.
 *
 * <ul>
 *   <li><strong>CONNECT, SUBSCRIBE 에러</strong>: STOMP ERROR 프레임 전송 (연결 차단)
 *   <li><strong>SEND 에러</strong>: 에러 채널 메시지 전송 (연결 유지)
 * </ul>
 *
 * <pre>
 * // JavaScript 클라이언트 예시:
 * const stompClient = Stomp.over(new SockJS('/ws-stomp'));
 * stompClient.connect({
 *     Authorization: 'Bearer ' + accessToken
 * }, function(frame) {
 *     // 연결 성공
 * });
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtWebSocketInterceptor implements ChannelInterceptor {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserDetailsServiceImpl userDetailsService;
  private final SimpMessagingTemplate messagingTemplate;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor != null) {
      StompCommand command = accessor.getCommand();

      try {
        if (StompCommand.CONNECT.equals(command)) {
          handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
          handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
          handleSend(accessor);
        }
      } catch (BusinessException e) {
        // SEND 에러는 에러 채널로 전송 (연결 유지)
        if (StompCommand.SEND.equals(command)) {
          sendErrorToChannel(accessor, e);
          return null; // 원본 메시지 전달 중단
        }

        // CONNECT, SUBSCRIBE 에러는 STOMP ERROR 프레임 (연결 차단 가능)
        return createErrorMessage(accessor, e);
      } catch (Exception e) {
        log.error("[WebSocket] 예상치 못한 예외 발생: {}", e.getMessage(), e);

        // SEND에서 예상치 못한 예외 발생 시에도 에러 채널로 전송
        if (StompCommand.SEND.equals(command)) {
          sendErrorToChannel(
              accessor, new BusinessException(WebSocketErrorCode.UNAUTHORIZED_CONNECTION));
          return null;
        }

        return createErrorMessage(
            accessor, new BusinessException(WebSocketErrorCode.UNAUTHORIZED_CONNECTION));
      }
    }

    return message;
  }

  /**
   * STOMP CONNECT 처리 - JWT 인증
   *
   * <p>다음과 같은 JWT 관련 예외를 모두 포착하여 일관된 WebSocket 에러 코드로 매핑합니다:
   *
   * <ul>
   *   <li>{@link BusinessException} - 비즈니스 로직 예외
   *   <li>{@link io.jsonwebtoken.JwtException} - JWT 파싱/검증 실패 (만료, 서명 불일치 등)
   *   <li>{@link IllegalArgumentException} - 잘못된 인자 (null 토큰, 빈 클레임 등)
   *   <li>{@link Exception} - 기타 예상치 못한 예외
   * </ul>
   *
   * @param accessor STOMP 헤더 접근자
   * @throws BusinessException WebSocket 인증 실패 시 (UNAUTHORIZED_CONNECTION)
   */
  private void handleConnect(StompHeaderAccessor accessor) {
    // STOMP CONNECT 프레임에서 Authorization 헤더 추출 (대소문자 무관)
    String authorizationHeader = getAuthorizationHeader(accessor);
    String token = getTokenFromHeader(authorizationHeader);

    if (!StringUtils.hasText(token)) {
      log.error("[WebSocket] STOMP CONNECT 시 Authorization 헤더가 없습니다.");
      throw new BusinessException(WebSocketErrorCode.UNAUTHORIZED_CONNECTION);
    }

    try {
      // JWT 토큰 검증
      jwtTokenProvider.validateAccessToken(token);
      String email =
          jwtTokenProvider.getClaimFromToken(token, "email", String.class, TokenType.ACCESS_TOKEN);

      // Principal 설정
      UserDetails userDetails = userDetailsService.loadUserByUsername(email);
      Authentication authentication =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

      accessor.setUser(authentication);

      log.info("[WebSocket] STOMP CONNECT 인증 성공 - userId: {}", userDetails.getUsername());

    } catch (BusinessException e) {
      log.error("[WebSocket] JWT 검증 실패 (BusinessException): {}", e.getMessage());
      throw new BusinessException(WebSocketErrorCode.UNAUTHORIZED_CONNECTION);
    } catch (io.jsonwebtoken.JwtException e) {
      log.error("[WebSocket] JWT 파싱/검증 실패 (JwtException): {}", e.getMessage());
      throw new BusinessException(WebSocketErrorCode.UNAUTHORIZED_CONNECTION);
    } catch (IllegalArgumentException e) {
      log.error("[WebSocket] JWT 인증 처리 실패 (IllegalArgumentException): {}", e.getMessage());
      throw new BusinessException(WebSocketErrorCode.UNAUTHORIZED_CONNECTION);
    } catch (Exception e) {
      log.error("[WebSocket] JWT 인증 처리 중 예상치 못한 오류: {}", e.getMessage(), e);
      throw new BusinessException(WebSocketErrorCode.UNAUTHORIZED_CONNECTION);
    }
  }

  /**
   * STOMP SUBSCRIBE 처리 - 사용자 인증 및 구독 권한 검증
   *
   * @param accessor STOMP 헤더 접근자
   */
  private void handleSubscribe(StompHeaderAccessor accessor) {
    // 사용자 인증 확인
    Authentication authentication = (Authentication) accessor.getUser();
    if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
      log.error("[WebSocket] SUBSCRIBE 시 인증되지 않은 사용자입니다.");
      throw new BusinessException(WebSocketErrorCode.UNAUTHORIZED_CONNECTION);
    }

    // UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String destination = accessor.getDestination();

    log.debug("[WebSocket] SUBSCRIBE 요청 - destination: {}", destination);

    // 구독 경로 검증 (에러 채널과 사용자별 채널만 허용)
    if (destination != null
        && !destination.startsWith("/user/sub/chat/")
        && !destination.startsWith("/user/sub/schedule/")
        && !destination.startsWith("/user/sub/errors/")) {
      log.error("[WebSocket] 허용되지 않은 구독 경로, destination: {}", destination);
      throw new BusinessException(WebSocketErrorCode.FORBIDDEN_SUBSCRIPTION);
    }
  }

  /**
   * STOMP SEND 처리 - 사용자 인증 확인
   *
   * @param accessor STOMP 헤더 접근자
   */
  private void handleSend(StompHeaderAccessor accessor) {
    // 사용자 인증 확인
    Authentication authentication = (Authentication) accessor.getUser();
    if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
      log.error("[WebSocket] SEND 시 인증되지 않은 사용자입니다.");
      throw new BusinessException(WebSocketErrorCode.UNAUTHORIZED_CONNECTION);
    }

    // UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String destination = accessor.getDestination();

    log.debug("[WebSocket] SEND 요청, destination: {}", destination);

    // 메시지 전송 경로 검증 (/pub/chat/{chatRoomId}/message만 허용)
    if (destination != null && !destination.matches("^/pub/chat/\\d+/message$")) {
      log.error("[WebSocket] 허용되지 않은 전송 경로, destination: {}", destination);
      throw new BusinessException(WebSocketErrorCode.FORBIDDEN_SEND);
    }
  }

  /**
   * STOMP 헤더에서 Authorization 헤더를 추출합니다 (대소문자 무관)
   *
   * @param accessor STOMP 헤더 접근자
   * @return Authorization 헤더 값, 없으면 null
   */
  private String getAuthorizationHeader(StompHeaderAccessor accessor) {
    // 대소문자 구분 없이 Authorization 헤더 검색
    String authHeader = accessor.getFirstNativeHeader("Authorization");
    if (authHeader != null) {
      return authHeader;
    }

    authHeader = accessor.getFirstNativeHeader("authorization");
    if (authHeader != null) {
      return authHeader;
    }

    // AUTHORIZATION도 시도
    return accessor.getFirstNativeHeader("AUTHORIZATION");
  }

  private String getTokenFromHeader(String authorizationHeader) {
    if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
      return authorizationHeader.substring(7);
    }
    return null;
  }

  /**
   * SEND 에러를 에러 채널로 전송합니다.
   *
   * <p>SEND 시점에는 이미 클라이언트가 에러 채널을 구독한 상태이므로, STOMP ERROR 프레임 대신 에러 채널 메시지로 전송하여 연결을 유지합니다.
   *
   * @param accessor STOMP 헤더 접근자
   * @param e 발생한 BusinessException
   */
  private void sendErrorToChannel(StompHeaderAccessor accessor, BusinessException e) {
    try {
      Authentication authentication = (Authentication) accessor.getUser();
      if (authentication == null) {
        log.error("[WebSocket] SEND 에러 전송 실패 - Principal이 null입니다.");
        return;
      }

      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String userId = userDetails.getUsername();

      // destination에서 chatRoomId 추출
      String destination = accessor.getDestination();
      Long chatRoomId = extractChatRoomId(destination);

      WebSocketErrorCode errorCode = (WebSocketErrorCode) e.getErrorCode();

      // 에러 응답 생성
      ErrorResponse errorResponse =
          ErrorResponse.of(errorCode.getCode(), errorCode.getDefaultMessage(), destination);

      // 에러 채널로 전송
      String errorChannel =
          chatRoomId != null ? "/sub/errors/" + chatRoomId : "/sub/errors/unknown";
      messagingTemplate.convertAndSendToUser(userId, errorChannel, errorResponse);

      log.info(
          "[WebSocket] SEND 에러를 에러 채널로 전송 완료 - userId: {}, channel: {}, code: {}",
          userId,
          errorChannel,
          errorCode.getCode());

    } catch (Exception ex) {
      log.error("[WebSocket] 에러 채널 전송 실패: {}", ex.getMessage(), ex);
    }
  }

  /**
   * destination에서 chatRoomId를 추출합니다.
   *
   * <p>destination 형식: /pub/chat/{chatRoomId}/message
   *
   * @param destination 메시지 전송 경로
   * @return chatRoomId, 추출 실패 시 null
   */
  private Long extractChatRoomId(String destination) {
    if (destination == null) {
      return null;
    }

    try {
      Pattern pattern = Pattern.compile("^/pub/chat/(\\d+)/message$");
      Matcher matcher = pattern.matcher(destination);
      if (matcher.matches()) {
        return Long.parseLong(matcher.group(1));
      }
    } catch (NumberFormatException e) {
      log.warn("[WebSocket] chatRoomId 파싱 실패 - destination: {}", destination);
    }

    return null;
  }

  /**
   * STOMP ERROR 프레임을 생성합니다.
   *
   * <p>ChannelInterceptor에서 발생한 예외를 STOMP 프로토콜 ERROR 프레임으로 변환하여 클라이언트에게 전송합니다.
   *
   * @param accessor STOMP 헤더 접근자
   * @param e 발생한 BusinessException
   * @return STOMP ERROR 프레임 메시지
   */
  private Message<?> createErrorMessage(StompHeaderAccessor accessor, BusinessException e) {
    WebSocketErrorCode errorCode = (WebSocketErrorCode) e.getErrorCode();

    log.error(
        "[WebSocket] STOMP ERROR 프레임 생성 - code: {}, message: {}",
        errorCode.getCode(),
        errorCode.getDefaultMessage());

    // STOMP ERROR 프레임 생성
    StompHeaderAccessor errorAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
    errorAccessor.setMessage(errorCode.getDefaultMessage());
    errorAccessor.setNativeHeader("error-code", errorCode.getCode());
    errorAccessor.setSessionId(accessor.getSessionId());
    errorAccessor.setLeaveMutable(true);

    return MessageBuilder.createMessage(
        errorCode.getDefaultMessage().getBytes(StandardCharsets.UTF_8),
        errorAccessor.getMessageHeaders());
  }
}
