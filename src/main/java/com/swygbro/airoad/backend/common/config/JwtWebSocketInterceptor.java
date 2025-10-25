package com.swygbro.airoad.backend.common.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.swygbro.airoad.backend.auth.application.UserDetailsServiceImpl;
import com.swygbro.airoad.backend.auth.domain.entity.TokenType;
import com.swygbro.airoad.backend.auth.filter.JwtTokenProvider;
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

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor != null) {
      StompCommand command = accessor.getCommand();

      if (StompCommand.CONNECT.equals(command)) {
        handleConnect(accessor);
      } else if (StompCommand.SUBSCRIBE.equals(command)) {
        handleSubscribe(accessor);
      } else if (StompCommand.SEND.equals(command)) {
        handleSend(accessor);
      }
    }

    return message;
  }

  /**
   * STOMP CONNECT 처리 - JWT 인증
   *
   * @param accessor STOMP 헤더 접근자
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
      log.error("[WebSocket] JWT 검증 실패: {}", e.getMessage());
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

    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String destination = accessor.getDestination();

    log.debug(
        "[WebSocket] SUBSCRIBE 요청 - userId: {}, destination: {}",
        userDetails.getUsername(),
        destination);

    // 구독 경로 검증 (에러 채널과 사용자별 채널만 허용)
    if (destination != null
        && !destination.startsWith("/user/sub/chat/")
        && !destination.startsWith("/user/sub/schedule/")
        && !destination.startsWith("/user/sub/errors/")) {
      log.error(
          "[WebSocket] 허용되지 않은 구독 경로 - userId: {}, destination: {}",
          userDetails.getUsername(),
          destination);
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

    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String destination = accessor.getDestination();

    log.debug(
        "[WebSocket] SEND 요청 - userId: {}, destination: {}",
        userDetails.getUsername(),
        destination);

    // 메시지 전송 경로 검증 (/pub/chat/{chatRoomId}/message만 허용)
    if (destination != null && !destination.matches("^/pub/chat/\\d+/message$")) {
      log.error(
          "[WebSocket] 허용되지 않은 전송 경로 - userId: {}, destination: {}",
          userDetails.getUsername(),
          destination);
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
}
