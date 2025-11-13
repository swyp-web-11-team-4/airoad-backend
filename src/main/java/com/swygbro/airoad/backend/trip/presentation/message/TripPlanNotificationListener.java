package com.swygbro.airoad.backend.trip.presentation.message;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.chat.domain.dto.response.ChatStreamDto;
import com.swygbro.airoad.backend.chat.domain.dto.response.MessageStreamType;
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
   * <p>WebSocket을 통해 일정 생성 채널과 채팅 채널로 일차별 일정 데이터를 전송합니다. 일정 생성 채널(/sub/schedule/{tripPlanId})에는 상세
   * 일정 데이터를, 채팅 채널(/sub/chat/{chatRoomId})에는 완료 메시지를 전송합니다.
   *
   * @param event 일차별 일정 저장 완료 이벤트
   */
  @EventListener
  public void handleDailyPlanSaved(DailyPlanSavedEvent event) {
    log.info(
        "일정 저장 완료 - tripPlanId: {}, dayNumber: {}",
        event.tripPlanId(),
        event.dailyPlan().dayNumber());

    TripPlanProgressMessage tripMessage =
        TripPlanProgressMessage.builder()
            .type(TripPlanProgressMessage.MessageType.DAILY_PLAN_GENERATED)
            .tripPlanId(event.tripPlanId())
            .dailyPlan(event.dailyPlan())
            .message(event.dailyPlan().dayNumber() + "일차 일정이 생성되었습니다.")
            .build();
    ChatStreamDto chatMessage =
        ChatStreamDto.of(
            event.dailyPlan().dayNumber() + "일차 일정이 생성되었습니다.",
            true,
            MessageStreamType.DAILY_PLAN_GENERATED);

    String tripDestination = "/sub/schedule/" + event.tripPlanId();
    String chatDestination = "/sub/chat/" + event.chatRoomId();
    sendToUser(event.username(), tripDestination, tripMessage);
    sendToUser(event.username(), chatDestination, chatMessage);
  }

  /**
   * 전체 일정 생성 완료 이벤트를 처리합니다.
   *
   * <p>WebSocket을 통해 채팅 채널로 완료 메시지를 전송합니다.
   *
   * @param event 전체 일정 생성 완료 이벤트
   */
  @EventListener
  public void handleTripPlanGenerationCompleted(TripPlanGenerationCompletedEvent event) {
    log.info("전체 일정 생성 완료 - tripPlanId: {}", event.tripPlanId());

    ChatStreamDto message = ChatStreamDto.of(event.message(), true, MessageStreamType.COMPLETED);

    String destination = "/sub/chat/" + event.chatRoomId();
    sendToUser(event.username(), destination, message);
  }

  /**
   * 일정 생성 오류 이벤트를 처리합니다.
   *
   * <p>WebSocket을 통해 에러 채널로 오류 메시지를 전송합니다.
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

    ChatStreamDto message =
        ChatStreamDto.of(event.errorCode().getDefaultMessage(), true, MessageStreamType.ERROR);

    String destination = "/sub/errors/" + event.chatRoomId();
    sendToUser(event.username(), destination, message);
  }

  /**
   * 일정 생성 취소 이벤트를 처리합니다.
   *
   * <p>WebSocket을 통해 채팅 채널로 취소 메시지를 전송합니다.
   *
   * @param event 일정 생성 취소 이벤트
   */
  @EventListener
  public void handleTripPlanGenerationCancelled(TripPlanGenerationCancelledEvent event) {
    log.info("일정 생성 취소 - tripPlanId: {}, reason: {}", event.tripPlanId(), event.reason());

    ChatStreamDto message =
        ChatStreamDto.of("일정 생성이 취소되었습니다: " + event.reason(), true, MessageStreamType.CANCELLED);

    String destination = "/sub/chat/" + event.chatRoomId();
    sendToUser(event.username(), destination, message);
  }

  /**
   * 특정 사용자에게 WebSocket 메시지를 전송합니다.
   *
   * @param username 사용자 이메일
   * @param destination WebSocket 목적지 경로 (예: /sub/chat/1, /sub/schedule/1, /sub/errors/1)
   * @param message 전송할 메시지
   */
  private void sendToUser(String username, String destination, Object message) {
    messagingTemplate.convertAndSendToUser(username, destination, message);
    log.debug(
        "WebSocket 메시지 전송 - destination: {}, type: {}",
        destination,
        message.getClass().getSimpleName());
  }
}
