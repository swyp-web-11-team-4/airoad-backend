package com.swygbro.airoad.backend.ai.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.swygbro.airoad.backend.ai.domain.dto.AiResponseContentType;
import com.swygbro.airoad.backend.ai.domain.event.AiRequestEvent;
import com.swygbro.airoad.backend.ai.domain.event.AiStreamChunkReceivedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 더미 AI 서비스
 *
 * <p>실제 AI 서버 연동 전까지 테스트 목적으로 사용되는 임시 서비스입니다. 고정된 더미 응답을 반환합니다.
 *
 * <p>TODO: 실제 AI 서비스 구현 시 이 클래스를 대체하거나 인터페이스 기반으로 리팩토링
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DummyAiService {

  private final ApplicationEventPublisher eventPublisher;

  /**
   * AI 요청을 처리하고 더미 응답을 이벤트로 발행합니다.
   *
   * @param event AI 요청 이벤트
   */
  public void processAiRequest(AiRequestEvent event) {
    log.info(
        "더미 AI 서비스 - 요청 처리 중 (chatRoomId: {}, userId: {})", event.chatRoomId(), event.userId());

    // 더미 응답 생성
    String dummyResponse = "안녕하세요! AI 어시스턴트입니다. 여행 일정에 대해 궁금하신 점을 말씀해 주세요. 현재는 테스트 응답입니다.";

    // AI 응답 이벤트 발행
    AiStreamChunkReceivedEvent responseEvent =
        new AiStreamChunkReceivedEvent(
            event.chatRoomId(),
            event.tripPlanId(),
            event.userId(),
            dummyResponse,
            AiResponseContentType.CHAT,
            true);

    eventPublisher.publishEvent(responseEvent);
    log.info("더미 AI 응답 이벤트 발행 완료 - chatRoomId: {}", event.chatRoomId());
  }
}
