package com.swygbro.airoad.backend.trip.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일정 장소 순서 교환 요청")
public record ScheduledPlaceSwapRequest(
    @Schema(description = "채팅방 ID", example = "456", requiredMode = Schema.RequiredMode.REQUIRED)
        Long chatRoomId,
    @Schema(
            description = "교환할 첫 번째 장소의 방문 순서",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Integer visitOrderA,
    @Schema(
            description = "교환할 두 번째 장소의 방문 순서",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Integer visitOrderB) {}
