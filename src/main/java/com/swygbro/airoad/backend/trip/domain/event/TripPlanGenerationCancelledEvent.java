package com.swygbro.airoad.backend.trip.domain.event;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

/**
 * 여행 일정 생성이 취소되었을 때 발행되는 이벤트입니다.
 *
 * <p>사용자가 진행 중인 일정 생성을 중단하거나, 세션이 종료되어 취소될 때 이 이벤트가 발행됩니다.
 */
@Getter
public class TripPlanGenerationCancelledEvent extends ApplicationEvent {

  /** WebSocket 세션 ID */
  private final String sessionId;

  /** 채팅방 ID */
  private final Long chatRoomId;

  /** 여행 일정 ID (생성되었을 경우) */
  private final Long tripPlanId;

  /** 취소 사유 */
  private final String reason;

  /**
   * 여행 일정 생성 취소 이벤트를 생성합니다.
   *
   * @param source 이벤트를 발행한 객체
   * @param sessionId WebSocket 세션 ID
   * @param chatRoomId 채팅방 ID
   * @param tripPlanId 여행 일정 ID (null 가능)
   * @param reason 취소 사유
   */
  public TripPlanGenerationCancelledEvent(
      Object source, String sessionId, Long chatRoomId, Long tripPlanId, String reason) {
    super(source);
    this.sessionId = sessionId;
    this.chatRoomId = chatRoomId;
    this.tripPlanId = tripPlanId;
    this.reason = reason;
  }
}
