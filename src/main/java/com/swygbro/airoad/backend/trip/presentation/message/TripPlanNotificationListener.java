package com.swygbro.airoad.backend.trip.presentation.message;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.trip.domain.dto.TripPlanProgressMessage;
import com.swygbro.airoad.backend.trip.domain.event.DailyPlanSavedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationCancelledEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationCompletedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationErrorEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripPlanNotificationListener {

  private final SimpMessagingTemplate messagingTemplate;

  /**
   * 일차별 일정 저장 완료 이벤트를 처리합니다.
   *
   * <p>WebSocket을 통해 해당 채팅방 구독자들에게 일차별 일정 데이터를 전송합니다.
   *
   * @param event 일차별 일정 저장 완료 이벤트
   */
  @EventListener
  public void handleDailyPlanSaved(DailyPlanSavedEvent event) {
    log.info(
        "일정 저장 완료 - tripPlanId: {}, dayNumber: {}",
        event.tripPlanId(),
        event.dailyPlan().dayNumber());

    TripPlanProgressMessage message =
        TripPlanProgressMessage.builder()
            .type(TripPlanProgressMessage.MessageType.DAILY_PLAN_GENERATED)
            .tripPlanId(event.tripPlanId())
            .dailyPlan(event.dailyPlan())
            .message(event.dailyPlan().dayNumber() + "일차 일정이 생성되었습니다.")
            .build();

    sendToUser(event.chatRoomId(), message);
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
    log.info("전체 일정 생성 완료 - tripPlanId: {}", event.tripPlanId());

    TripPlanProgressMessage message =
        TripPlanProgressMessage.builder()
            .type(TripPlanProgressMessage.MessageType.COMPLETED)
            .tripPlanId(event.tripPlanId())
            .message(event.message())
            .build();

    sendToUser(event.chatRoomId(), message);
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
        event.tripPlanId(),
        event.errorCode().getCode(),
        event.errorCode().getDefaultMessage());

    TripPlanProgressMessage message =
        TripPlanProgressMessage.builder()
            .type(TripPlanProgressMessage.MessageType.ERROR)
            .tripPlanId(event.tripPlanId())
            .message(event.errorCode().getDefaultMessage())
            .errorCode(event.errorCode().getCode())
            .build();

    sendToUser(event.chatRoomId(), message);
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
    log.info("일정 생성 취소 - tripPlanId: {}, reason: {}", event.tripPlanId(), event.reason());

    TripPlanProgressMessage message =
        TripPlanProgressMessage.builder()
            .type(TripPlanProgressMessage.MessageType.CANCELLED)
            .tripPlanId(event.tripPlanId())
            .message("일정 생성이 취소되었습니다: " + event.reason())
            .build();

    sendToUser(event.chatRoomId(), message);
  }

  /**
   * 특정 사용자의 채팅방으로 메시지를 전송합니다.
   *
   * @param chatRoomId 채팅방 ID
   * @param message 전송할 메시지
   */
  private void sendToUser(Long chatRoomId, TripPlanProgressMessage message) {
    String destination = "/sub/chatroom/" + chatRoomId + "/trip-progress";
    messagingTemplate.convertAndSend(destination, message);
    log.debug("WebSocket 메시지 전송 - destination: {}, type: {}", destination, message.type());
  }
}
