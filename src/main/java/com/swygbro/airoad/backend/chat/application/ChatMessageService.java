package com.swygbro.airoad.backend.chat.application;

import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageRequest;
import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageResponse;
import com.swygbro.airoad.backend.chat.domain.dto.MessageType;
import com.swygbro.airoad.backend.chat.domain.entity.Sender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 채팅 메시지 처리 서비스
 *
 * <p>AI와의 1:1 채팅 메시지를 처리하고 WebSocket을 통해 응답을 전송합니다.
 *
 * <h3>실시간 메시지 전송</h3>
 * <p>이 서비스는 {@code SimpMessagingTemplate.convertAndSendToUser()}를 사용하여
 * 특정 사용자의 구독 경로로 메시지를 전송합니다.
 *
 * <ul>
 *   <li><strong>전송 경로</strong>: {@code /user/{userId}/sub/chat/{chatRoomId}}
 *   <li><strong>경로 구조</strong>: {@code /user} prefix + userId + destination
 *   <li><strong>destination</strong>: {@code /sub/chat/{chatRoomId}}
 * </ul>
 *
 * <p><strong>다른 실시간 메시징 타입</strong>: 일정 알림, 일반 알림 등은
 * {@link com.swygbro.airoad.backend.realtime} 패키지 문서를 참조하세요.
 *
 * @see com.swygbro.airoad.backend.realtime
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService implements ChatMessageUseCase {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void processAndSendMessage(Long chatRoomId, String userId, ChatMessageRequest request) {
        log.info("[WebSocket] 메시지 수신 - chatRoomId: {}, userId: {}", chatRoomId, userId);

        // TODO: 1. DB에 메시지 저장
        // aiMessageRepository.save(userMessage);

        // TODO: 2. AI 응답 생성
        // ChatMessageResponse aiResponse = aiService.generateResponse(chatRoomId, userId,
        // request);

        // ===== 테스트용 임시 더미 응답 (실제 AI 구현 전까지만 사용) =====
        ChatMessageResponse response =
                new ChatMessageResponse(
                        999L, // messageId (임시)
                        Sender.AI,
                        "[테스트] AI 응답: " + request.content(), // 사용자 메시지를 그대로 에코
                        MessageType.TEXT,
                        null, // mediaUrl
                        java.time.LocalDateTime.now());
        // ================================================================

        // TODO: 3. AI 응답 DB 저장
        // aiMessageRepository.save(aiMessage);

        // 4. WebSocket을 통해 사용자의 특정 채팅방 구독 경로로 AI 응답 전송
        // 실제 경로: /user/{userId}/sub/chat/{chatRoomId}
        String destination = "/sub/chat/" + chatRoomId;
        messagingTemplate.convertAndSendToUser(userId, destination, response);

        log.info("[WebSocket] AI 응답 전송 완료 - chatRoomId: {}, userId: {}", chatRoomId, userId);
    }
}
