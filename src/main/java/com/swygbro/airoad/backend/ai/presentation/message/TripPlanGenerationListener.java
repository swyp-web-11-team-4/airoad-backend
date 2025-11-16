package com.swygbro.airoad.backend.ai.presentation.message;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.application.common.AiUseCase;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationRequestedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 여행 일정 생성 요청 이벤트를 수신하는 리스너입니다. */
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
  @Async
  @EventListener
  public void handleTripPlanGenerationRequested(TripPlanGenerationRequestedEvent event) {
    log.info(
        "여행 일정 생성 요청 이벤트 수신 - chatRoomId: {}, tripPlanId: {}",
        event.chatRoomId(),
        event.tripPlanId());

    AiDailyPlanRequest request =
        AiDailyPlanRequest.builder()
            .chatRoomId(event.chatRoomId())
            .tripPlanId(event.tripPlanId())
            .username(event.username())
            .themes(event.request().themes())
            .startDate(event.request().startDate())
            .duration(event.request().duration())
            .region(event.request().region())
            .peopleCount(event.request().peopleCount())
            .transportation(Transportation.PUBLIC_TRANSIT)
            .build();

    aiUseCase.agentCall(AgentType.TRIP_AGENT, request);
  }
}
