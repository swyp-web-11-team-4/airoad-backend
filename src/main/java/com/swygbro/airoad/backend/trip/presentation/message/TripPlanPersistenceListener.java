package com.swygbro.airoad.backend.trip.presentation.message;

import com.swygbro.airoad.backend.ai.domain.event.DailyPlanGeneratedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripPlanPersistenceListener {

  @EventListener
  public void handleDailyPlanGenerated(DailyPlanGeneratedEvent event) {
    log.debug(
        "AI {}일차 일정 생성 완료 - chatRoomId: {}, tripPlanId: {}",
        event.dailyPlan().dayNumber(),
        event.chatRoomId(),
        event.tripPlanId()
    );
  }
}
