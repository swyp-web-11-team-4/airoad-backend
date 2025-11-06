package com.swygbro.airoad.backend.trip.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 여행 일정 생성 요청 응답 DTO */
@Schema(description = "여행 일정 생성 요청 응답")
public record ChannelIdResponse(
    @Schema(description = "생성된 AI 대화방 ID", example = "456") Long conversationId,
    @Schema(description = "생성된 여행 일정 ID", example = "789") Long tripPlanId) {}
