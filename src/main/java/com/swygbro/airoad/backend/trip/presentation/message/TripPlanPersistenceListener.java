package com.swygbro.airoad.backend.trip.presentation.message;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.domain.event.DailyPlanGeneratedEvent;
import com.swygbro.airoad.backend.trip.application.DailyPlanUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripPlanPersistenceListener {

  private final DailyPlanUseCase dailyPlanUseCase;

  @EventListener
  public void handleDailyPlanGenerated(DailyPlanGeneratedEvent event) {
    log.debug(
        "AI {}일차 일정 생성 완료 - chatRoomId: {}, tripPlanId: {}",
        event.dailyPlan().dayNumber(),
        event.chatRoomId(),
        event.tripPlanId());

    dailyPlanUseCase.saveDailyPlan(event.chatRoomId(), event.tripPlanId(), event.dailyPlan());
  }
}
