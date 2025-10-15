package com.swygbro.airoad.backend.chat.domain.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public class ChatRoomDto {

  @Schema(description = "채팅방 생성 요청 DTO")
  public record CreateRequest(
      @Schema(description = "채팅방 제목", example = "서울 여행 계획") @NotBlank(message = "채팅방 제목은 필수입니다.")
          String title) {}

  @Schema(description = "채팅방 응답 DTO")
  public record Response(
      @Schema(description = "채팅방 ID", example = "1") Long id,
      @Schema(description = "채팅방 제목", example = "서울 여행 계획") String title,
      @Schema(description = "마지막 메시지 시간", example = "2025-01-15T10:30:00")
          LocalDateTime lastMessageAt,
      @Schema(description = "생성 일시", example = "2025-01-15T10:30:00") LocalDateTime createdAt) {

    /**
     * 채팅방 엔티티로부터 ChatRoomDto.Response를 생성합니다.
     *
     * @param chatRoom 채팅방 엔티티
     * @return ChatRoomDto.Response
     */
    // TODO: Implement when ChatRoom entity is created
    // public static Response from(ChatRoom chatRoom) {
    //   return new Response(
    //       chatRoom.getId(),
    //       chatRoom.getTitle(),
    //       chatRoom.getLastMessage(),
    //       chatRoom.getLastMessageAt(),
    //       chatRoom.getCreatedAt()
    //   );
    // }
  }
}
