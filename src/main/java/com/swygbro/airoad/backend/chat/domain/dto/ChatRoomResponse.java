package com.swygbro.airoad.backend.chat.domain.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅방 응답 DTO")
public record ChatRoomResponse(
    @Schema(description = "채팅방 ID", example = "1") Long id,
    @Schema(description = "채팅방 제목", example = "서울 여행 계획") String title,
    @Schema(description = "마지막 메시지 시간", example = "2025-01-15T10:30:00")
        LocalDateTime lastMessageAt,
    @Schema(description = "생성 일시", example = "2025-01-15T10:30:00") LocalDateTime createdAt) {

  /**
   * 채팅방 엔티티로부터 ChatRoomResponse를 생성합니다.
   *
   * @param chatRoom 채팅방 엔티티
   * @return ChatRoomResponse
   */
  // TODO: Implement when ChatRoom entity is created
  // public static ChatRoomResponse from(ChatRoom chatRoom) {
  //   return new ChatRoomResponse(
  //       chatRoom.getId(),
  //       chatRoom.getTitle(),
  //       chatRoom.getLastMessage(),
  //       chatRoom.getLastMessageAt(),
  //       chatRoom.getCreatedAt()
  //   );
  // }
}
