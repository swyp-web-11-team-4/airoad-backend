package com.swygbro.airoad.backend.chat.presentation.message;

import org.springframework.ai.chat.messages.MessageType;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.domain.event.DailyPlanGeneratedEvent;
import com.swygbro.airoad.backend.chat.application.AiMessageCommandUseCase;
import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageCreateRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPersistenceListener {

  private final AiMessageCommandUseCase aiMessageCommandUseCase;

  @EventListener
  public void handleDailyPlanGenerated(DailyPlanGeneratedEvent event) {

    ChatMessageCreateRequest request =
        ChatMessageCreateRequest.builder()
            .messageType(MessageType.ASSISTANT)
            .message(event.dailyPlan().description())
            .build();

    aiMessageCommandUseCase.saveMessage(event.chatRoomId(), request);
  }
}
