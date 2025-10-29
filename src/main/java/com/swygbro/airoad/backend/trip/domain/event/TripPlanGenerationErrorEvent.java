package com.swygbro.airoad.backend.trip.domain.event;

import org.springframework.context.ApplicationEvent;

import com.swygbro.airoad.backend.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 여행 일정 생성 중 오류가 발생했을 때 발행되는 이벤트입니다.
 *
 * <p>AI 스트리밍 실패, 파싱 오류, 타임아웃 등의 오류 발생 시 이 이벤트가 발행됩니다.
 */
@Getter
public class TripPlanGenerationErrorEvent extends ApplicationEvent {

  /** WebSocket 세션 ID */
  private final String sessionId;

  /** 채팅방 ID */
  private final Long chatRoomId;

  /** 여행 일정 ID (생성되었을 경우) */
  private final Long tripPlanId;

  /** 에러 코드 */
  private final ErrorCode errorCode;

  /** 에러 메시지 */
  private final String errorMessage;

  /** 발생한 예외 (선택적) */
  private final Throwable throwable;

  /**
   * 여행 일정 생성 오류 이벤트를 생성합니다.
   *
   * @param source 이벤트를 발행한 객체
   * @param sessionId WebSocket 세션 ID
   * @param chatRoomId 채팅방 ID
   * @param tripPlanId 여행 일정 ID (null 가능)
   * @param errorCode 에러 코드
   * @param errorMessage 에러 메시지
   * @param throwable 발생한 예외 (null 가능)
   */
  public TripPlanGenerationErrorEvent(
      Object source,
      String sessionId,
      Long chatRoomId,
      Long tripPlanId,
      ErrorCode errorCode,
      String errorMessage,
      Throwable throwable) {
    super(source);
    this.sessionId = sessionId;
    this.chatRoomId = chatRoomId;
    this.tripPlanId = tripPlanId;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
    this.throwable = throwable;
  }
}
