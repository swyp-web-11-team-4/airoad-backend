package com.swygbro.airoad.backend.chat.presentation;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageDto;

import lombok.extern.slf4j.Slf4j;

/**
 * STOMP WebSocket 기반 실시간 채팅 메시지 컨트롤러
 *
 * <p>클라이언트 연결 방법:
 *
 * <ul>
 *   <li>WebSocket 엔드포인트: /ws-stomp
 *   <li>메시지 전송: /pub/chatroom/{roomId}/message
 *   <li>메시지 구독: /sub/chatroom/{roomId}
 *   <li>입장 알림: /pub/chatroom/{roomId}/enter
 *   <li>퇴장 알림: /pub/chatroom/{roomId}/leave
 * </ul>
 *
 * <p>TODO: WebSocketConfig 설정 후 @Autowired(required = false) 제거 필요
 */
@Slf4j
@Controller
public class ChatMessageController {

  // WebSocket Config가 설정되기 전까지 일시적으로 required = false 설정
  @Autowired(required = false)
  private SimpMessagingTemplate messagingTemplate;

  /**
   * 채팅 메시지 전송 처리
   *
   * <p>클라이언트가 /pub/chatroom/{roomId}/message로 메시지를 전송하면 해당 채팅방을 구독 중인 모든 클라이언트에게 브로드캐스트합니다.
   *
   * @param chatRoomId 채팅방 ID
   * @param messageRequest 메시지 요청 DTO
   * @param sessionId WebSocket 세션 ID
   */
  @MessageMapping("/chatroom/{chatRoomId}/message")
  public void sendMessage(
      @DestinationVariable Long chatRoomId,
      @Valid @Payload ChatMessageDto.Request messageRequest,
      @Header("simpSessionId") String sessionId) {

    log.info(
        "메시지 수신 - roomId: {}, sessionId: {}, content: {}",
        chatRoomId,
        sessionId,
        messageRequest.content());

    // TODO: 서비스 레이어에서 메시지 처리 (DB 저장, AI 응답 생성 등)
    // ChatMessageDto.Response response = chatMessageService.processMessage(roomId,
    // messageRequest, sessionId);

    // 임시 응답 (실제 구현 시 서비스에서 생성된 응답 사용)
    ChatMessageDto.Response response = null;

    // 해당 채팅방을 구독 중인 모든 클라이언트에게 메시지 브로드캐스트
    if (messagingTemplate != null) {
      messagingTemplate.convertAndSend("/sub/chatroom/" + chatRoomId, response);
      log.info(
          "메시지 전송 완료 - roomId: {}, messageId: {}", chatRoomId, response != null ? "N/A" : "N/A");
    } else {
      log.warn("SimpMessagingTemplate이 아직 설정되지 않았습니다. WebSocketConfig를 설정해주세요.");
    }
  }

  /**
   * 채팅방 입장 처리
   *
   * <p>사용자가 채팅방에 입장할 때 호출됩니다. 입장 알림을 해당 채팅방 구독자들에게 전송합니다.
   *
   * @param chatRoomId 채팅방 ID
   * @param sessionId WebSocket 세션 ID
   */
  @MessageMapping("/chatroom/{chatroomId}/enter")
  public void enterChatRoom(
      @DestinationVariable Long chatRoomId, @Header("simpSessionId") String sessionId) {

    log.info("채팅방 입장 - roomId: {}, sessionId: {}", chatRoomId, sessionId);

    // TODO: 입장 처리 로직 (Redis에 세션 정보 저장, 참여자 수 업데이트 등)
    // chatRoomService.enterRoom(roomId, sessionId);

    // 입장 알림 전송 (선택사항)
    // messagingTemplate.convertAndSend("/topic/chatroom/" + roomId + "/enter", enterNotification);
  }
}
