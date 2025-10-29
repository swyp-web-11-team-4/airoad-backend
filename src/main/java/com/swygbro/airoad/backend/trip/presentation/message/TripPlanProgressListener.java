package com.swygbro.airoad.backend.trip.presentation.message;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.trip.domain.dto.TripPlanProgressMessage;
import com.swygbro.airoad.backend.trip.domain.event.DailyPlanGeneratedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationCancelledEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationCompletedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationErrorEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 여행 일정 생성 진행 상황 이벤트를 수신하여 WebSocket으로 클라이언트에 전송하는 리스너입니다.
 *
 * <p>일차별 완료, 전체 완료, 오류, 취소 등의 이벤트를 수신하여 STOMP 메시지로 변환 후 전송합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TripPlanProgressListener {

  private final SimpMessagingTemplate messagingTemplate;

  /**
   * 일차별 일정 생성 완료 이벤트를 처리합니다.
   *
   * <p>WebSocket을 통해 해당 채팅방 구독자들에게 일차별 일정 데이터를 전송합니다.
   *
   * @param event 일차별 일정 생성 이벤트
   */
  @EventListener
  public void handleDailyPlanGenerated(DailyPlanGeneratedEvent event) {
    log.info(
        "일차별 일정 생성 완료 - tripPlanId: {}, dayNumber: {}",
        event.getTripPlanId(),
        event.getDailyPlan().dayNumber());

    TripPlanProgressMessage message =
        TripPlanProgressMessage.builder()
            .type(TripPlanProgressMessage.MessageType.DAILY_PLAN_GENERATED)
            .tripPlanId(event.getTripPlanId())
            .dailyPlan(event.getDailyPlan())
            .message(event.getDailyPlan().dayNumber() + "일차 일정이 생성되었습니다.")
            .build();

    sendToUser(event.getSessionId(), event.getChatRoomId(), message);
  }

  /**
   * 전체 일정 생성 완료 이벤트를 처리합니다.
   *
   * <p>WebSocket을 통해 완료 메시지를 전송합니다.
   *
   * @param event 전체 일정 생성 완료 이벤트
   */
  @EventListener
  public void handleTripPlanGenerationCompleted(TripPlanGenerationCompletedEvent event) {
    log.info("전체 일정 생성 완료 - tripPlanId: {}", event.getTripPlanId());

    TripPlanProgressMessage message =
        TripPlanProgressMessage.builder()
            .type(TripPlanProgressMessage.MessageType.COMPLETED)
            .tripPlanId(event.getTripPlanId())
            .message(event.getMessage())
            .build();

    sendToUser(event.getSessionId(), event.getChatRoomId(), message);
  }

  /**
   * 일정 생성 오류 이벤트를 처리합니다.
   *
   * <p>WebSocket을 통해 오류 메시지를 전송합니다.
   *
   * @param event 일정 생성 오류 이벤트
   */
  @EventListener
  public void handleTripPlanGenerationError(TripPlanGenerationErrorEvent event) {
    log.error(
        "일정 생성 오류 발생 - tripPlanId: {}, errorCode: {}, message: {}",
        event.getTripPlanId(),
        event.getErrorCode().getCode(),
        event.getErrorMessage(),
        event.getThrowable());

    TripPlanProgressMessage message =
        TripPlanProgressMessage.builder()
            .type(TripPlanProgressMessage.MessageType.ERROR)
            .tripPlanId(event.getTripPlanId())
            .message(event.getErrorMessage())
            .errorCode(event.getErrorCode().getCode())
            .build();

    sendToUser(event.getSessionId(), event.getChatRoomId(), message);
  }

  /**
   * 일정 생성 취소 이벤트를 처리합니다.
   *
   * <p>WebSocket을 통해 취소 메시지를 전송합니다.
   *
   * @param event 일정 생성 취소 이벤트
   */
  @EventListener
  public void handleTripPlanGenerationCancelled(TripPlanGenerationCancelledEvent event) {
    log.info("일정 생성 취소 - tripPlanId: {}, reason: {}", event.getTripPlanId(), event.getReason());

    TripPlanProgressMessage message =
        TripPlanProgressMessage.builder()
            .type(TripPlanProgressMessage.MessageType.CANCELLED)
            .tripPlanId(event.getTripPlanId())
            .message("일정 생성이 취소되었습니다: " + event.getReason())
            .build();

    sendToUser(event.getSessionId(), event.getChatRoomId(), message);
  }

  /**
   * 특정 사용자의 채팅방으로 메시지를 전송합니다.
   *
   * @param sessionId WebSocket 세션 ID
   * @param chatRoomId 채팅방 ID
   * @param message 전송할 메시지
   */
  private void sendToUser(String sessionId, Long chatRoomId, TripPlanProgressMessage message) {
    String destination = "/sub/chatroom/" + chatRoomId + "/trip-progress";
    messagingTemplate.convertAndSend(destination, message);
    log.debug("WebSocket 메시지 전송 - destination: {}, type: {}", destination, message.type());
  }
}
