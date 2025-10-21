package com.swygbro.airoad.backend.chat.domain.event;

import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageResponse;

/**
 * AI 메시지 저장 완료 이벤트
 *
 * <p>AI 메시지가 DB에 저장된 후 발행되며, WebSocket 전송 등 후속 처리를 트리거합니다.
 *
 * @param chatRoomId 채팅방 ID
 * @param userId 사용자 ID
 * @param response 저장된 메시지 응답 DTO
 */
public record AiMessageSavedEvent(Long chatRoomId, String userId, ChatMessageResponse response) {}
