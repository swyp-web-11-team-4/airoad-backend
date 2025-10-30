package com.swygbro.airoad.backend.ai.presentation.message;

import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.application.AiUseCase;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 여행 일정 생성 요청 이벤트를 수신하는 리스너입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TripPlanGenerationListener {

  private final AiUseCase aiUseCase;

  /**
   * 여행 일정 생성 요청 이벤트를 처리합니다.
   *
   * @param event 여행 일정 생성 요청 이벤트
   */
  @EventListener
  public void handleTripPlanGenerationRequested(TripPlanGenerationRequestedEvent event) {
    log.info("여행 일정 생성 요청 이벤트 수신 - chatRoomId: {}, tripPlanId: {}", event.chatRoomId(),
        event.tripPlanId());

    AiDailyPlanRequest request = AiDailyPlanRequest.builder()
        .chatRoomId(event.chatRoomId())
        .tripPlanId(event.tripPlanId())
        .themes(event.request().themes())
        .startDate(event.request().startDate())
        .duration(event.request().duration())
        .region(event.request().region())
        .peopleCount(event.request().peopleCount())
        .build();

    aiUseCase.agentCall("tripAgent", request);
  }
}
