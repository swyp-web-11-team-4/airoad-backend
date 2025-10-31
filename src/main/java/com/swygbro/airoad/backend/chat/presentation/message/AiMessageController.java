package com.swygbro.airoad.backend.chat.presentation.message;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import com.swygbro.airoad.backend.chat.application.AiMessageService;
import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageRequest;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.common.exception.WebSocketErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * STOMP WebSocket 기반 AI 1:1 채팅 컨트롤러
 *
 * <p>AI와의 실시간 채팅 메시지를 처리하는 컨트롤러입니다.
 *
 * <h3>WebSocket 연결 (Handshake)</h3>
 *
 * <ul>
 *   <li><strong>엔드포인트</strong>: {@code /ws-stomp}
 *   <li><strong>프로토콜</strong>: SockJS + STOMP
 *   <li><strong>인증</strong>: STOMP CONNECT 프레임에 {@code Authorization: Bearer <JWT>} 헤더 필수
 * </ul>
 *
 * <h3>메시지 발행 (Client → Server)</h3>
 *
 * <ul>
 *   <li><strong>경로</strong>: {@code /pub/chat/{chatRoomId}/message}
 *   <li><strong>페이로드</strong>: {@link ChatMessageRequest}
 *   <li><strong>인증</strong>: JWT 토큰 검증 완료된 사용자만 전송 가능
 *   <li><strong>권한</strong>: 채팅방 소유자만 메시지 전송 가능 (email 기반 검증)
 * </ul>
 *
 * <h3>메시지 구독 (Server → Client)</h3>
 *
 * <ul>
 *   <li><strong>채팅 응답</strong>: {@code /user/sub/chat/{tripPlanId}}
 *   <li><strong>일정 응답</strong>: {@code /user/sub/schedule/{tripPlanId}}
 *   <li><strong>페이로드</strong>: {@link
 *       com.swygbro.airoad.backend.chat.domain.dto.ChatMessageResponse}
 *   <li><strong>설명</strong>: tripPlanId별로 구분된 AI 응답을 실시간으로 수신 (사용자 email 기반 라우팅)
 * </ul>
 *
 * <h3>에러 메시지 구독 (Server → Client)</h3>
 *
 * <ul>
 *   <li><strong>경로</strong>: {@code /user/sub/errors}
 *   <li><strong>페이로드</strong>: {@link com.swygbro.airoad.backend.common.domain.dto.ErrorResponse}
 *   <li><strong>설명</strong>: WebSocket 메시지 처리 중 발생한 에러를 실시간으로 수신 ({@link WebSocketExceptionHandler}
 *       참조)
 * </ul>
 *
 * <h3>클라이언트 연동 예시</h3>
 *
 * <pre>{@code
 * // 1. WebSocket 연결
 * const socket = new SockJS('/ws-stomp');
 * const stompClient = Stomp.over(socket);
 *
 * // 2. STOMP CONNECT (JWT 인증)
 * stompClient.connect({
 *     Authorization: 'Bearer ' + accessToken
 * }, function(frame) {
 *     console.log('Connected: ' + frame);
 *
 *     // 3. 메시지 구독
 *     stompClient.subscribe('/user/sub/chat/1', function(message) {
 *         const response = JSON.parse(message.body);
 *         console.log('AI Response:', response);
 *     });
 *
 *     // 4. 에러 구독
 *     stompClient.subscribe('/user/sub/errors', function(error) {
 *         const errorResponse = JSON.parse(error.body);
 *         console.error('Error:', errorResponse);
 *     });
 *
 *     // 5. 메시지 전송
 *     stompClient.send('/pub/chat/1/message', {}, JSON.stringify({
 *         content: '안녕하세요',
 *         messageContentType: 'TEXT'
 *     }));
 * });
 * }</pre>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AiMessageController {
  private final AiMessageService aiMessageService;

  /**
   * 채팅 메시지 전송 처리 (AI와의 1:1 대화)
   *
   * <p>클라이언트가 /pub/chat/{chatRoomId}/message로 메시지를 전송하면, 다음 순서로 처리됩니다:
   *
   * <ol>
   *   <li>STOMP 메시지에서 페이로드 및 인증 정보 추출
   *   <li>인증 정보 검증 (UserDetails 타입 확인)
   *   <li>채팅방 존재 여부 확인
   *   <li>사용자 권한 검증 (채팅방 소유자인지 email 기반 확인)
   *   <li>메시지 타입 검증 (현재 TEXT만 지원)
   *   <li>AI 요청 이벤트 발행 (AiRequestEvent)
   *   <li>AI 응답을 /user/sub/chat/{chatRoomId}로 실시간 전송
   * </ol>
   *
   * @param chatRoomId 채팅방 ID
   * @param message STOMP 메시지 (페이로드: {@link ChatMessageRequest})
   * @param headerAccessor STOMP 헤더 접근자 (인증 정보 포함)
   * @throws com.swygbro.airoad.backend.common.exception.BusinessException WS001 - 인증되지 않은 연결
   * @throws com.swygbro.airoad.backend.common.exception.BusinessException CHAT301 - 존재하지 않는 채팅방
   * @throws com.swygbro.airoad.backend.common.exception.BusinessException CHAT302 - 채팅방 접근 권한 없음
   * @throws com.swygbro.airoad.backend.common.exception.BusinessException CHAT102 - 지원하지 않는 메시지 타입
   */
  @MessageMapping("/chat/{chatRoomId}/message")
  public void sendMessage(
      @DestinationVariable Long chatRoomId,
      Message<ChatMessageRequest> message,
      StompHeaderAccessor headerAccessor) {

    // 페이로드 추출
    ChatMessageRequest messageRequest = message.getPayload();

    // 인증 정보 추출
    Authentication authentication = (Authentication) headerAccessor.getUser();
    if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
      log.error("[Controller] Authentication is null - chatRoomId: {}", chatRoomId);
      throw new BusinessException(WebSocketErrorCode.UNAUTHORIZED_CONNECTION);
    }

    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String userId = userDetails.getUsername();

    log.info(
        "[Controller] 메시지 수신 - chatRoomId: {}, content: {}", chatRoomId, messageRequest.content());

    // 서비스 레이어에서 메시지 처리 및 WebSocket 응답 전송
    aiMessageService.processAndSendMessage(chatRoomId, userId, messageRequest);
  }
}
