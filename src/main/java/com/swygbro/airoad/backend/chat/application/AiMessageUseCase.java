package com.swygbro.airoad.backend.chat.application;

import com.swygbro.airoad.backend.chat.domain.dto.request.ChatMessageRequest;
import com.swygbro.airoad.backend.chat.domain.dto.response.ChatMessageResponse;
import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;

/**
 * 채팅 메시지 처리 유스케이스
 *
 * <p>AI와의 1:1 채팅 메시지를 처리하고 WebSocket을 통해 응답을 전송합니다.
 */
public interface AiMessageUseCase {

  /**
   * 채팅 메시지를 처리하고 AI 응답을 WebSocket으로 전송
   *
   * @param chatRoomId 채팅방 ID
   * @param username 사용자 이름, 이메일
   * @param request 채팅 메시지 요청
   */
  void processAndSendMessage(Long chatRoomId, String username, ChatMessageRequest request);

  /**
   * 채팅방의 메시지 히스토리를 커서 기반 페이지네이션으로 조회
   *
   * @param chatRoomId 채팅방 ID
   * @param username 사용자 이름, 이메일 (권한 검증용)
   * @param cursor 커서 (메시지 ID, null이면 최신 메시지부터 조회)
   * @param size 조회할 메시지 개수
   * @return 메시지 히스토리 커서 페이지 응답
   */
  CursorPageResponse<ChatMessageResponse> getMessageHistory(
      Long chatRoomId, String username, Long cursor, int size);
}
