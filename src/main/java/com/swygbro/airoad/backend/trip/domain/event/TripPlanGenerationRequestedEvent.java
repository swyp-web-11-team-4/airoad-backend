package com.swygbro.airoad.backend.trip.domain.event;

import org.springframework.context.ApplicationEvent;

import com.swygbro.airoad.backend.trip.domain.dto.TripGenerationRequest;

import lombok.Getter;

/**
 * 여행 일정 생성이 요청되었을 때 발행되는 이벤트입니다.
 *
 * <p>사용자가 여행 일정 생성을 요청하면 이 이벤트가 발행되고, AI 리스너가 이를 수신하여 일정 생성 프로세스를 시작합니다.
 */
@Getter
public class TripPlanGenerationRequestedEvent extends ApplicationEvent {

  /** WebSocket 세션 ID */
  private final String sessionId;

  /** 채팅방 ID */
  private final Long chatRoomId;

  /** 회원 ID */
  private final Long memberId;

  /** 여행 일정 생성 요청 정보 */
  private final TripGenerationRequest request;

  /**
   * 여행 일정 생성 요청 이벤트를 생성합니다.
   *
   * @param source 이벤트를 발행한 객체
   * @param sessionId WebSocket 세션 ID
   * @param chatRoomId 채팅방 ID
   * @param memberId 회원 ID
   * @param request 여행 일정 생성 요청 정보
   */
  public TripPlanGenerationRequestedEvent(
      Object source,
      String sessionId,
      Long chatRoomId,
      Long memberId,
      TripGenerationRequest request) {
    super(source);
    this.sessionId = sessionId;
    this.chatRoomId = chatRoomId;
    this.memberId = memberId;
    this.request = request;
  }
}
