package com.swygbro.airoad.backend.trip.domain.event;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

/**
 * 전체 여행 일정 생성이 완료되었을 때 발행되는 이벤트입니다.
 *
 * <p>모든 일차별 일정이 생성되고 TripPlan의 isCompleted가 true로 업데이트되면 이 이벤트가 발행됩니다.
 */
@Getter
public class TripPlanGenerationCompletedEvent extends ApplicationEvent {

  /** WebSocket 세션 ID */
  private final String sessionId;

  /** 채팅방 ID */
  private final Long chatRoomId;

  /** 여행 일정 ID */
  private final Long tripPlanId;

  /** 완료 메시지 */
  private final String message;

  /**
   * 여행 일정 생성 완료 이벤트를 생성합니다.
   *
   * @param source 이벤트를 발행한 객체
   * @param sessionId WebSocket 세션 ID
   * @param chatRoomId 채팅방 ID
   * @param tripPlanId 여행 일정 ID
   * @param message 완료 메시지
   */
  public TripPlanGenerationCompletedEvent(
      Object source, String sessionId, Long chatRoomId, Long tripPlanId, String message) {
    super(source);
    this.sessionId = sessionId;
    this.chatRoomId = chatRoomId;
    this.tripPlanId = tripPlanId;
    this.message = message;
  }
}
