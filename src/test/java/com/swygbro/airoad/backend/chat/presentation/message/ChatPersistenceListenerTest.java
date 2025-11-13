package com.swygbro.airoad.backend.chat.presentation.message;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.MessageType;

import com.swygbro.airoad.backend.ai.domain.event.DailyPlanGeneratedEvent;
import com.swygbro.airoad.backend.chat.application.AiMessageCommandUseCase;
import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.DailyPlanCreateRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatPersistenceListenerTest {

  @InjectMocks private ChatPersistenceListener chatPersistenceListener;

  @Mock private AiMessageCommandUseCase aiMessageCommandUseCase;

  @Test
  @DisplayName("DailyPlanGeneratedEvent 발생 시, AI 어시스턴트 메시지가 저장되어야 한다.")
  void handleDailyPlanGenerated_Success() {
    // given
    Long chatRoomId = 1L;
    Long tripPlanId = 1L;
    String username = "test@example.com";
    String description = "서울 1일차 추천 일정입니다.";

    DailyPlanCreateRequest dailyPlan =
        DailyPlanCreateRequest.builder()
            .dayNumber(1)
            .date(LocalDate.now())
            .title("서울 1일차")
            .description(description)
            .places(Collections.emptyList())
            .build();

    DailyPlanGeneratedEvent event =
        DailyPlanGeneratedEvent.builder()
            .chatRoomId(chatRoomId)
            .tripPlanId(tripPlanId)
            .username(username)
            .dailyPlan(dailyPlan)
            .build();

    ChatMessageCreateRequest expectedRequest =
        ChatMessageCreateRequest.builder()
            .messageType(MessageType.ASSISTANT)
            .message(description)
            .build();

    // when
    chatPersistenceListener.handleDailyPlanGenerated(event);

    // then
    verify(aiMessageCommandUseCase)
        .saveMessage(eq(chatRoomId), any(ChatMessageCreateRequest.class));
    verify(aiMessageCommandUseCase).saveMessage(chatRoomId, expectedRequest);
  }
}
