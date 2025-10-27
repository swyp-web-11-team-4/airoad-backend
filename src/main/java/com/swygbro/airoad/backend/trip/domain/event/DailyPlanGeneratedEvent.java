package com.swygbro.airoad.backend.trip.domain.event;

import org.springframework.context.ApplicationEvent;

import com.swygbro.airoad.backend.trip.domain.dto.DailyPlanDto;

import lombok.Getter;

/**
 * 일차별 여행 일정이 생성되었을 때 발행되는 이벤트입니다.
 *
 * <p>AI가 하루치 일정을 완성하면 이 이벤트가 발행되고, WebSocket을 통해 클라이언트로 실시간 전송됩니다.
 */
@Getter
public class DailyPlanGeneratedEvent extends ApplicationEvent {

  /** WebSocket 세션 ID */
  private final String sessionId;

  /** 채팅방 ID */
  private final Long chatRoomId;

  /** 여행 일정 ID */
  private final Long tripPlanId;

  /** 생성된 일차별 일정 데이터 */
  private final DailyPlanDto dailyPlan;

  /**
   * 일차별 일정 생성 이벤트를 생성합니다.
   *
   * @param source 이벤트를 발행한 객체
   * @param sessionId WebSocket 세션 ID
   * @param chatRoomId 채팅방 ID
   * @param tripPlanId 여행 일정 ID
   * @param dailyPlan 생성된 일차별 일정 데이터
   */
  public DailyPlanGeneratedEvent(
      Object source, String sessionId, Long chatRoomId, Long tripPlanId, DailyPlanDto dailyPlan) {
    super(source);
    this.sessionId = sessionId;
    this.chatRoomId = chatRoomId;
    this.tripPlanId = tripPlanId;
    this.dailyPlan = dailyPlan;
  }
}
