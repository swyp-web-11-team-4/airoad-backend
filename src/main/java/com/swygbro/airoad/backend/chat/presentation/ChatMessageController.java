package com.swygbro.airoad.backend.chat.presentation;

import java.security.Principal;

import jakarta.validation.Valid;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
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
 * <h3>메시지 발행 (Client → Server)</h3>
 *
 * <ul>
 *   <li><strong>경로</strong>: {@code /pub/chat/{chatRoomId}/message}
 *   <li><strong>페이로드</strong>: {@link ChatMessageRequest}
 *   <li><strong>인증</strong>: Principal 필요 (Spring Security Context)
 * </ul>
 *
 * <h3>메시지 구독 (Server → Client)</h3>
 *
 * <ul>
 *   <li><strong>경로</strong>: {@code /user/sub/chat/{chatRoomId}}
 *   <li><strong>페이로드</strong>: {@link
 *       com.swygbro.airoad.backend.chat.domain.dto.ChatMessageResponse}
 *   <li><strong>설명</strong>: 해당 채팅방의 AI 응답을 실시간으로 수신
 * </ul>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AiMessageController {
  private final AiMessageService aiMessageService;

  /**
   * 채팅 메시지 전송 처리 (AI와의 1:1 대화)
   *
   * <p>클라이언트가 /pub/chat/{chatRoomId}/message로 메시지를 전송하면 서비스 레이어에서 AI 응답을 생성하여 해당 사용자의 채팅방 구독 경로로
   * 전달합니다.
   *
   * @param chatRoomId 채팅방 ID
   * @param messageRequest 메시지 요청 DTO
   * @param principal 인증된 사용자 정보 (userId)
   * @throws com.swygbro.airoad.backend.common.exception.BusinessException WS001 - 인증되지 않은 연결
   * @throws com.swygbro.airoad.backend.common.exception.BusinessException CHAT301 - 존재하지 않는 채팅방
   * @throws com.swygbro.airoad.backend.common.exception.BusinessException CHAT102 - 지원하지 않는 메시지 타입
   */
  @MessageMapping("/chat/{chatRoomId}/message")
  public void sendMessage(
      @DestinationVariable Long chatRoomId,
      @Valid @Payload ChatMessageRequest messageRequest,
      Principal principal) {

    // 인증되지 않은 사용자는 채팅 불가 (방어적 코드)
    if (principal == null) {
      log.error("[Controller] Principal is null - chatRoomId: {}", chatRoomId);
      throw new BusinessException(WebSocketErrorCode.UNAUTHORIZED_CONNECTION);
    }

    String userId = principal.getName();

    log.info(
        "[Controller] 메시지 수신 - chatRoomId: {}, userId: {}, content: {}",
        chatRoomId,
        userId,
        messageRequest.content());

    // 서비스 레이어에서 메시지 처리 및 WebSocket 응답 전송
    aiMessageService.processAndSendMessage(chatRoomId, userId, messageRequest);
  }
}
