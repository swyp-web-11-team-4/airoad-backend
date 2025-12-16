package com.swygbro.airoad.backend.trip.domain.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일정 장소 순서 교환 요청")
public record ScheduledPlaceSwapRequest(
    @Schema(description = "채팅방 ID", example = "456", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "채팅방 ID는 필수입니다.")
        Long chatRoomId,
    @Schema(
            description = "교환할 첫 번째 장소의 방문 순서",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "첫 번째 방문 순서는 필수입니다.")
        @Positive(message = "방문 순서는 1 이상이어야 합니다.")
        Integer visitOrderA,
    @Schema(
            description = "교환할 두 번째 장소의 방문 순서",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "두 번째 방문 순서는 필수입니다.")
        @Positive(message = "방문 순서는 1 이상이어야 합니다.")
        Integer visitOrderB) {

  @AssertTrue(message = "교환할 두 장소의 순서가 같을 수 없습니다.")
  @Schema(hidden = true)
  public boolean isVisitOrderDifferent() {
    if (visitOrderA == null || visitOrderB == null) {
      return true;
    }
    return !visitOrderA.equals(visitOrderB);
  }
}
