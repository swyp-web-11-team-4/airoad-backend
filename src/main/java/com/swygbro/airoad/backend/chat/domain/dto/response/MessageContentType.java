package com.swygbro.airoad.backend.chat.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메시지 타입")
public enum MessageContentType {
  @Schema(description = "텍스트 메시지")
  TEXT,

  @Schema(description = "이미지 메시지")
  IMAGE,

  @Schema(description = "파일 메시지")
  FILE,

  @Schema(description = "위치 정보 메시지")
  LOCATION,

  @Schema(description = "시스템 메시지")
  SYSTEM
}
