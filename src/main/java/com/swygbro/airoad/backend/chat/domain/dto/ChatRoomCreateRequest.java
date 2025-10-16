package com.swygbro.airoad.backend.chat.domain.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅방 생성 요청 DTO")
public record ChatRoomCreateRequest(
    @Schema(description = "채팅방 제목", example = "서울 여행 계획") @NotBlank(message = "채팅방 제목은 필수입니다.")
        String title) {}
