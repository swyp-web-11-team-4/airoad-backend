package com.swygbro.airoad.backend.chat.domain.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public class ChatMessageDto {

  @Schema(description = "채팅 메시지 전송 요청 DTO")
  public record Request(
      @Schema(description = "메시지 내용", example = "서울 3박 4일 여행 계획을 짜주세요")
          @NotBlank(message = "메시지 내용은 필수입니다.")
          String content,
      @Schema(description = "메시지 타입", example = "TEXT", defaultValue = "TEXT")
          MessageType messageType) {}

  @Schema(description = "채팅 메시지 응답 DTO")
  public record Response(
      @Schema(description = "메시지 ID", example = "1") Long id,
      @Schema(description = "발신자 타입 (USER 또는 AI)", example = "USER") SenderType senderType,
      @Schema(description = "메시지 내용", example = "서울 3박 4일 여행 계획을 짜주세요") String content,
      @Schema(description = "메시지 타입", example = "TEXT") MessageType messageType,
      @Schema(
              description = "미디어 URL (이미지/파일)",
              nullable = true,
              example = "https://example.com/image.jpg")
          String mediaUrl,
      @Schema(description = "생성 일시", example = "2025-01-15T10:30:00") LocalDateTime createdAt) {

    /**
     * 메시지 엔티티로부터 ChatMessageDto.Response를 생성합니다.
     *
     * @param message 메시지 엔티티
     * @return ChatMessageDto.Response
     */
    // TODO: Implement when ChatMessage entity is created
    // public static Response from(ChatMessage message) {
    //   return new Response(
    //       message.getId(),
    //       message.getSenderType(),
    //       message.getContent(),
    //       message.getCreatedAt()
    //   );
    // }
  }

  public enum SenderType {
    USER,
    AI
  }
}
