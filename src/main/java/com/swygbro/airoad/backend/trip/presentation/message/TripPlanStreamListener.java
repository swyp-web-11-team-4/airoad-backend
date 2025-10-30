package com.swygbro.airoad.backend.trip.presentation.message;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.domain.dto.AiResponseContentType;
import com.swygbro.airoad.backend.ai.domain.event.AiStreamChunkReceivedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 응답 스트림을 여행 일정 데이터로 변환하는 핸들러입니다.
 *
 * <p>{@link AiStreamChunkReceivedEvent}를 수신하여 SCHEDULE 타입 응답을 처리합니다. 스트리밍 청크를 버퍼링하고, 일차별 완성 여부를
 * 감지하여 파싱 후 데이터베이스에 저장합니다.
 *
 * <h3>주요 책임</h3>
 *
 * <ul>
 *   <li>SCHEDULE 타입 응답 필터링 (CHAT 타입 무시)
 *   <li>스트리밍 청크 버퍼링 및 일차별 구분
 *   <li>DailyPlanDto 파싱 및 검증
 *   <li>DailyPlan 및 ScheduledPlace 엔티티 저장
 *   <li>DailyPlanGeneratedEvent 발행 (WebSocket 전송 트리거)
 *   <li>전체 완료 시 TripPlanGenerationCompletedEvent 발행
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TripPlanStreamListener {

  private final ApplicationEventPublisher eventPublisher;

  /**
   * AI 스트림 청크 수신 이벤트를 처리합니다.
   *
   * <p>contentType이 SCHEDULE인 경우, 여행 일정 생성 관련 응답으로 처리합니다.
   *
   * @param event AI 스트림 청크 이벤트
   */
  @EventListener
  public void handleAiStreamChunkReceived(AiStreamChunkReceivedEvent event) {
    log.debug(
        "AI 응답 수신 - chatRoomId: {}, tripPlanId: {}, contentType: {}, isComplete: {}",
        event.chatRoomId(),
        event.tripPlanId(),
        event.contentType(),
        event.isComplete());

    if (event.contentType() == AiResponseContentType.CHAT) {
      log.debug("CHAT 타입 응답 무시 - 여행 일정과 무관");
      return;
    }

    if (event.contentType() == AiResponseContentType.SCHEDULE) {
      handleScheduleStream(event);
    }
  }

  /**
   * SCHEDULE 타입 응답 스트림을 처리합니다.
   *
   * <p>스켈레톤 구현으로 현재는 로깅만 수행합니다. 향후 구현 예정 기능:
   *
   * <ul>
   *   <li>청크 버퍼링 및 일차별 구분자 감지
   *   <li>DailyPlanDto 파싱 및 검증
   *   <li>데이터베이스 저장 (DailyPlan + ScheduledPlace)
   *   <li>도메인 이벤트 발행 (DailyPlanGeneratedEvent, TripPlanGenerationCompletedEvent)
   * </ul>
   *
   * @param event AI 스트림 청크 이벤트
   */
  private void handleScheduleStream(AiStreamChunkReceivedEvent event) {
    log.info("여행 일정 스트림 수신 - tripPlanId: {}, 완료 여부: {}", event.tripPlanId(), event.isComplete());

    // TODO: 실제 일정 생성 시 DailyPlanDto로 교체 예정
    // 현재는 더미 String 데이터
    if (event.contentType() == AiResponseContentType.SCHEDULE) {
      log.debug("일정 응답 내용 (처음 100자): {}", event.content());
    }
  }
}
