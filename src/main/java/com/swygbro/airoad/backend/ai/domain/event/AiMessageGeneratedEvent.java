package com.swygbro.airoad.backend.ai.domain.event;

import lombok.Builder;

@Builder
public record AiMessageGeneratedEvent(
    Long chatRoomId,
    Long tripPlanId,
    String username,
    String aiMessage
) {

}
