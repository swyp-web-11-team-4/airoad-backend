package com.swygbro.airoad.backend.ai.presentation.message;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.domain.event.AiRequestEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 채팅 요청 이벤트를 수신하는 리스너입니다.
 *
 * <p>AiRequestEvent를 수신하여 AI 서비스를 통한 채팅 처리 프로세스를 시작하고, AI 응답을 스트리밍으로 전달합니다.
 *
 * <h3>이벤트 흐름</h3>
 *
 * <ol>
 *   <li>사용자가 채팅 메시지 전송 → {@link com.swygbro.airoad.backend.chat.application.AiMessageService}에서
 *       {@link AiRequestEvent} 발행
 *   <li>이 리스너가 이벤트 수신
 *   <li>AI 서비스 호출 (TODO: 다음 이슈에서 구현 예정)
 *   <li>AI 응답을 {@link com.swygbro.airoad.backend.ai.domain.event.AiStreamChunkReceivedEvent}로 발행
 *   <li>{@link com.swygbro.airoad.backend.chat.presentation.message.AiResponseEventListener}가
 *       WebSocket으로 전송
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiChatRequestListener {

  /**
   * AI 채팅 요청 이벤트를 처리합니다.
   *
   * <p>비동기로 실행되며, AI 서비스를 호출하여 채팅 응답을 생성합니다.
   *
   * @param event AI 채팅 요청 이벤트
   */
  @Async
  @EventListener
  public void handleAiChatRequest(AiRequestEvent event) {
    log.info(
        "AI 채팅 요청 이벤트 수신 - chatRoomId: {}, tripPlanId: {}, userId: {}",
        event.chatRoomId(),
        event.tripPlanId(),
        event.userId());

    log.debug("AI 채팅 요청 메시지: {}", event.userMessage());

    // TODO: AI 서비스 호출 및 응답 처리 (다음 이슈에서 구현)
    // 1. AI 서비스에 메시지 전송
    // 2. 스트리밍 응답 수신
    // 3. 각 청크마다 AiStreamChunkReceivedEvent 발행
  }
}
