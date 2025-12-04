package com.swygbro.airoad.backend.trip.presentation.message;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.swygbro.airoad.backend.chat.domain.dto.response.ChatStreamDto;
import com.swygbro.airoad.backend.chat.domain.dto.response.MessageStreamType;
import com.swygbro.airoad.backend.common.domain.dto.ErrorResponse;
import com.swygbro.airoad.backend.trip.domain.dto.TripPlanProgressMessage;
import com.swygbro.airoad.backend.trip.domain.dto.TripPlanProgressMessage.MessageType;
import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;
import com.swygbro.airoad.backend.trip.domain.event.DailyPlanSavedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationCancelledEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationCompletedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationErrorEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanUpdateStartedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanUpdatedEvent;

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
   * <p>트랜잭션 커밋 후 WebSocket을 통해 일정 생성 채널과 채팅 채널로 일차별 일정 데이터를 전송합니다. 일정 생성
   * 채널(/sub/schedule/{tripPlanId})에는 상세 일정 데이터를, 채팅 채널(/sub/chat/{chatRoomId})에는 완료 메시지를 전송합니다.
   *
   * <p>@TransactionalEventListener를 사용하여 트랜잭션 커밋 후 이벤트를 처리함으로써, 프론트엔드가 항상 최신 DB 데이터를 받을 수 있도록
   * 보장합니다.
   *
   * @param event 일차별 일정 저장 완료 이벤트
   */
  @TransactionalEventListener
  public void handleDailyPlanSaved(DailyPlanSavedEvent event) {
    log.info(
        "일정 저장 완료 - tripPlanId: {}, dayNumber: {}",
        event.tripPlanId(),
        event.dailyPlan().dayNumber());

    TripPlanProgressMessage<DailyPlanResponse> tripMessage =
        TripPlanProgressMessage.<DailyPlanResponse>builder()
            .type(TripPlanProgressMessage.MessageType.DAILY_PLAN_GENERATED)
            .tripPlanId(event.tripPlanId())
            .data(event.dailyPlan())
            .message(event.dailyPlan().dayNumber() + "일차 일정이 생성되었습니다.")
            .build();
    ChatStreamDto chatMessage =
        ChatStreamDto.of(event.dailyPlan().description(), true, MessageStreamType.CHAT);

    String tripDestination =
        "/queue/schedule-" + event.tripPlanId() + "-" + event.username();
    String chatDestination = "/queue/chat-" + event.chatRoomId() + "-" + event.username();
    sendMessage(tripDestination, tripMessage);
    sendMessage(chatDestination, chatMessage);
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

    String destination = "/queue/chat-" + event.chatRoomId() + "-" + event.username();
    sendMessage(destination, message);
  }

  /**
   * 일정 생성 오류 이벤트를 처리합니다.
   *
   * <p>WebSocket을 통해 에러 채널로 오류 메시지를 전송합니다.
   *
   * @param event 일정 생성 오류 이벤트
   */
  @TransactionalEventListener
  public void handleTripPlanGenerationError(TripPlanGenerationErrorEvent event) {
    log.error(
        "일정 생성 오류 발생 - tripPlanId: {}, errorCode: {}, message: {}",
        event.tripPlanId(),
        event.errorCode().getCode(),
        event.errorCode().getDefaultMessage());

    String destination = "/queue/errors-" + event.chatRoomId() + "-" + event.username();
    ErrorResponse message =
        ErrorResponse.of(
            event.errorCode().getCode(), event.errorCode().getDefaultMessage(), destination);

    sendMessage(destination, message);
  }

  /**
   * 일정 생성 취소 이벤트를 처리합니다.
   *
   * <p>WebSocket을 통해 채팅 채널로 취소 메시지를 전송합니다.
   *
   * @param event 일정 생성 취소 이벤트
   */
  @TransactionalEventListener
  public void handleTripPlanGenerationCancelled(TripPlanGenerationCancelledEvent event) {
    log.info("일정 생성 취소 - tripPlanId: {}, reason: {}", event.tripPlanId(), event.reason());

    ChatStreamDto message =
        ChatStreamDto.of("일정 생성이 취소되었습니다: " + event.reason(), true, MessageStreamType.CANCELLED);

    String destination = "/queue/chat-" + event.chatRoomId() + "-" + event.username();
    sendMessage(destination, message);
  }

  /**
   * 일정 수정 시작 이벤트를 처리합니다.
   *
   * <p>WebSocket을 통해 일정 채널로 수정 시작 메시지를 전송합니다.
   *
   * @param event 일정 수정 시작 이벤트
   */
  @TransactionalEventListener
  public void handleTripPlanUpdateStarted(TripPlanUpdateStartedEvent event) {
    log.info("일정 수정 시작 - tripPlanId: {}, message: {}", event.tripPlanId(), event.message());

    TripPlanProgressMessage<List<Long>> tripMessage =
        TripPlanProgressMessage.<List<Long>>builder()
            .type(TripPlanProgressMessage.MessageType.UPDATE_STARTED)
            .tripPlanId(event.tripPlanId())
            .data(event.scheduledPlaceIdList())
            .message(event.message())
            .build();

    String destination = "/queue/schedule-" + event.tripPlanId() + "-" + event.username();
    sendMessage(destination, tripMessage);
  }

  /**
   * 일정 수정 완료 이벤트를 처리합니다.
   *
   * <p>트랜잭션 커밋 후 WebSocket을 통해 일정 채널과 채팅 채널로 수정된 일정 데이터를 전송합니다. 일정 채널(/queue/schedule-{tripPlanId}-{username})에는
   * 수정된 일정 데이터를, 채팅 채널(/queue/chat-{chatRoomId}-{username})에는 완료 메시지를 전송합니다.
   *
   * <p>@TransactionalEventListener를 사용하여 트랜잭션 커밋 후 이벤트를 처리함으로써, 프론트엔드가 항상 최신 DB 데이터를 받을 수 있도록
   * 보장합니다.
   *
   * @param event 일정 수정 완료 이벤트
   */
  @TransactionalEventListener
  public void handleTripPlanUpdated(TripPlanUpdatedEvent event) {
    log.info(
        "일정 수정 완료 - tripPlanId: {}, dayNumber: {}",
        event.tripPlanId(),
        event.dailyPlan().dayNumber());

    TripPlanProgressMessage<DailyPlanResponse> tripMessage =
        TripPlanProgressMessage.<DailyPlanResponse>builder()
            .type(MessageType.UPDATED)
            .tripPlanId(event.tripPlanId())
            .data(event.dailyPlan())
            .message(event.dailyPlan().dayNumber() + "일차 일정이 수정되었습니다.")
            .build();
    ChatStreamDto chatMessage =
        ChatStreamDto.of(event.dailyPlan().description(), true, MessageStreamType.UPDATED);

    String tripDestination =
        "/queue/schedule-" + event.tripPlanId() + "-" + event.username();
    String chatDestination = "/queue/chat-" + event.chatRoomId() + "-" + event.username();
    sendMessage(tripDestination, tripMessage);
    sendMessage(chatDestination, chatMessage);
  }

  /**
   * WebSocket 메시지를 전송합니다.
   *
   * @param destination WebSocket 목적지 경로 (예: /queue/chat-370-username)
   * @param message 전송할 메시지
   */
  private void sendMessage(String destination, Object message) {
    messagingTemplate.convertAndSend(destination, message);
    log.debug(
        "WebSocket 메시지 전송 - destination: {}, type: {}",
        destination,
        message.getClass().getSimpleName());
  }
}
