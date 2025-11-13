package com.swygbro.airoad.backend.chat.domain.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 채팅 메시지 스트리밍 응답 DTO
 *
 * <p>WebSocket을 통해 실시간으로 전송되는 AI 채팅 응답입니다.
 */
@Schema(description = "채팅 메시지 스트리밍 응답")
public record ChatStreamDto(
    @Schema(description = "메시지 내용 (스트리밍 청크)", example = "안녕하세요! AI 어시스턴트입니다.") String message,
    @Schema(description = "스트리밍 완료 여부", example = "false") boolean isComplete,
    @Schema(description = "응답 생성 시각", example = "2025-01-15T10:30:00") LocalDateTime timestamp,
    @Schema(description = "메세지 스티림 타입", example = "DAILY_PLAN_GENERATED")
        MessageStreamType messageStreamType) {

  /**
   * AI 응답 메시지로부터 Chat 타입의 ChatStreamDto를 생성합니다.
   *
   * @param message AI 응답 메시지
   * @param isComplete 스트리밍 완료 여부
   * @return ChatStreamDto
   */
  public static ChatStreamDto ofChat(String message, boolean isComplete) {
    return new ChatStreamDto(message, isComplete, LocalDateTime.now(), MessageStreamType.CHAT);
  }

  public static ChatStreamDto of(
      String message, boolean isComplete, MessageStreamType messageStreamType) {
    return new ChatStreamDto(message, isComplete, LocalDateTime.now(), messageStreamType);
  }
}
