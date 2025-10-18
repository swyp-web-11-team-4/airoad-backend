package com.swygbro.airoad.backend.chat.application;

import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageRequest;

/**
 * 채팅 메시지 처리 유스케이스
 *
 * <p>AI와의 1:1 채팅 메시지를 처리하고 WebSocket을 통해 응답을 전송합니다.
 */
public interface ChatMessageUseCase {

    /**
     * 채팅 메시지를 처리하고 AI 응답을 WebSocket으로 전송
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @param request 채팅 메시지 요청
     */
    void processAndSendMessage(Long chatRoomId, String userId, ChatMessageRequest request);
}
