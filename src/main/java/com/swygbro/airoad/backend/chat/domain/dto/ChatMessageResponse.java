package com.swygbro.airoad.backend.chat.domain.dto;

import java.time.LocalDateTime;

import com.swygbro.airoad.backend.chat.domain.entity.Sender;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅 메시지 응답 DTO")
public record ChatMessageResponse(
    @Schema(description = "메시지 ID", example = "1") Long id,
    @Schema(description = "발신자 타입 (USER, AI, TOOL)", example = "USER") Sender sender,
    @Schema(description = "메시지 내용", example = "서울 3박 4일 여행 계획을 짜주세요") String content,
    @Schema(description = "메시지 타입", example = "TEXT") MessageContentType messageContentType,
    @Schema(
            description = "미디어 URL (이미지/파일)",
            nullable = true,
            example = "https://example.com/image.jpg")
        String mediaUrl,
    @Schema(description = "생성 일시", example = "2025-01-15T10:30:00") LocalDateTime createdAt) {

  /*
   * 메시지 엔티티로부터 ChatMessageResponse를 생성합니다.
   *
   * @param message 메시지 엔티티
   * @return ChatMessageResponse
   */
  // TODO: Implement when ChatMessage entity is created
  // public static ChatMessageResponse from(ChatMessage message) {
  //   return new ChatMessageResponse(
  //       message.getId(),
  //       message.getSender(),
  //       message.getContent(),
  //       message.getMessageType(),
  //       message.getMediaUrl(),
  //       message.getCreatedAt()
  //   );
  // }
}
