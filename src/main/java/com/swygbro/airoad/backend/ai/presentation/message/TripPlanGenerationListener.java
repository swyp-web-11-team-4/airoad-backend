package com.swygbro.airoad.backend.ai.presentation.message;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationRequestedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 여행 일정 생성 요청 이벤트를 수신하는 리스너입니다.
 *
 * <p>TripPlanGenerationRequestedEvent를 수신하여 AI 서비스를 통한 일정 생성 프로세스를 시작하고, 생성 결과에 따라 적절한 도메인 이벤트를
 * 발행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TripPlanGenerationListener {

  private final ApplicationEventPublisher eventPublisher;

  /**
   * 여행 일정 생성 요청 이벤트를 처리합니다.
   *
   * <p>비동기로 실행되며, AI 서비스를 호출하여 일정 생성을 시작합니다.
   *
   * @param event 여행 일정 생성 요청 이벤트
   */
  @Async
  @EventListener
  public void handleTripPlanGenerationRequested(TripPlanGenerationRequestedEvent event) {
    log.info(
        "여행 일정 생성 요청 이벤트 수신 - sessionId: {}, chatRoomId: {}, memberId: {}",
        event.getSessionId(),
        event.getChatRoomId(),
        event.getMemberId());

    log.debug("여행 일정 생성 요청 데이터: {}", event.getRequest());
  }
}
