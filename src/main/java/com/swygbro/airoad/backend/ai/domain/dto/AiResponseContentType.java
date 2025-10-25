package com.swygbro.airoad.backend.ai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 응답 콘텐츠 타입
 *
 * <p>AI 서버로부터 받은 응답의 유형을 구분합니다. 이를 통해 적절한 WebSocket 채널로 라우팅됩니다.
 */
@Schema(description = "AI 응답 콘텐츠 타입")
public enum AiResponseContentType {
  /**
   * 일반 채팅 메시지
   *
   * <p>채널: {@code /user/sub/chat/{chatRoomId}}
   */
  @Schema(description = "일반 채팅 메시지")
  CHAT,

  /**
   * 여행 일정 데이터
   *
   * <p>채널: {@code /user/sub/schedule}
   */
  @Schema(description = "여행 일정 데이터")
  SCHEDULE
}
