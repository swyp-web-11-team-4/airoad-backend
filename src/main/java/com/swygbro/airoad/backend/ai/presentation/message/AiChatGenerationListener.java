package com.swygbro.airoad.backend.ai.presentation.message;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.chat.dto.request.AiChatRequest;
import com.swygbro.airoad.backend.ai.application.AiUseCase;
import com.swygbro.airoad.backend.chat.domain.event.AiChatGenerationRequestedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 채팅 요청 이벤트를 수신하는 리스너입니다.
 *
 * <p>AiChatGenerationEvent를 수신하여 AI 서비스를 통한 채팅 처리 프로세스를 시작하고, AI 응답을 스트리밍으로 전달합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiChatGenerationListener {

  private final AiUseCase aiUseCase;

  /**
   * AI 채팅 요청 이벤트를 처리합니다.
   *
   * @param event AI 채팅 요청 이벤트
   */
  @EventListener
  public void handleAiChatRequest(AiChatGenerationRequestedEvent event) {
    log.info(
        "AI 채팅 요청 이벤트 수신 - chatRoomId: {}, tripPlanId: {}", event.chatRoomId(), event.tripPlanId());

    AiChatRequest request =
        AiChatRequest.builder()
            .chatRoomId(event.chatRoomId())
            .tripPlanId(event.tripPlanId())
            .username(event.username())
            .userPrompt(event.userMessage())
            .build();

    aiUseCase.agentCall("chatAgent", request);
  }
}
