package com.swygbro.airoad.backend.chat.domain.dto.request;

import jakarta.validation.constraints.NotBlank;

import com.swygbro.airoad.backend.chat.domain.dto.response.MessageContentType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅 메시지 전송 요청 DTO")
public record ChatMessageRequest(
    @Schema(description = "메시지 내용", example = "서울 3박 4일 여행 계획을 짜주세요")
        @NotBlank(message = "메시지 내용은 필수입니다.")
        String content,
    @Schema(description = "메시지 타입", example = "TEXT", defaultValue = "TEXT")
        MessageContentType messageContentType) {

  // 기본값을 TEXT로 설정하는 compact 생성자
  public ChatMessageRequest {
    if (messageContentType == null) {
      messageContentType = MessageContentType.TEXT;
    }
  }
}
