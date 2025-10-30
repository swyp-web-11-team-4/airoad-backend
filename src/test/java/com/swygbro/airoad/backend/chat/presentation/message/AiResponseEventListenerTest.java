package com.swygbro.airoad.backend.chat.presentation.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.ai.domain.dto.AiResponseContentType;
import com.swygbro.airoad.backend.ai.domain.event.AiStreamChunkReceivedEvent;
import com.swygbro.airoad.backend.chat.domain.dto.ChatStreamDto;
import com.swygbro.airoad.backend.trip.domain.dto.DailyPlanDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

/**
 * AiResponseEventListener 테스트
 *
 * <p>AI 응답 수신 이벤트 처리 및 WebSocket 전송 로직을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("AiResponseEventListener 클래스")
class AiResponseEventListenerTest {

  @Mock private SimpMessagingTemplate messagingTemplate;

  @InjectMocks private AiResponseEventListener eventListener;

  @Captor private ArgumentCaptor<ChatStreamDto> chatDtoCaptor;

  @Captor private ArgumentCaptor<DailyPlanDto> dailyPlanDtoCaptor;

  @Nested
  @DisplayName("handleAiStreamChunkReceived 메서드는")
  class HandleAiStreamChunkReceived {

    @Test
    @DisplayName("CHAT 타입 응답을 채팅 채널로 전송한다")
    void shouldSendChatResponseToCorrectChannel() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 10L;
      String userId = "user123@example.com";
      String content = "서울 3박 4일 여행 일정을 생성했습니다.";
      String expectedDestination = "/sub/chat/" + chatRoomId;
      AiStreamChunkReceivedEvent event =
          new AiStreamChunkReceivedEvent(
              chatRoomId, tripPlanId, userId, content, AiResponseContentType.CHAT, false);

      // when
      eventListener.handleAiStreamChunkReceived(event);

      // then
      then(messagingTemplate)
          .should()
          .convertAndSendToUser(eq(userId), eq(expectedDestination), chatDtoCaptor.capture());

      ChatStreamDto capturedDto = chatDtoCaptor.getValue();
      assertThat(capturedDto.message()).isEqualTo(content);
      assertThat(capturedDto.sender()).isEqualTo("AI");
      assertThat(capturedDto.isComplete()).isFalse();
      assertThat(capturedDto.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("SCHEDULE 타입 응답을 일정 채널로 전송한다")
    void shouldSendScheduleResponseToCorrectChannel() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 20L;
      String userId = "user123@example.com";

      DailyPlanDto dailyPlan =
          DailyPlanDto.builder()
              .dayNumber(1)
              .date(java.time.LocalDate.now())
              .places(java.util.List.of())
              .build();

      String expectedDestination = "/sub/schedule/" + tripPlanId;
      AiStreamChunkReceivedEvent event =
          new AiStreamChunkReceivedEvent(
              chatRoomId, tripPlanId, userId, dailyPlan, AiResponseContentType.SCHEDULE, false);

      // when
      eventListener.handleAiStreamChunkReceived(event);

      // then
      then(messagingTemplate)
          .should()
          .convertAndSendToUser(eq(userId), eq(expectedDestination), dailyPlanDtoCaptor.capture());

      DailyPlanDto capturedDto = dailyPlanDtoCaptor.getValue();
      assertThat(capturedDto.dayNumber()).isEqualTo(1);
      assertThat(capturedDto.date()).isNotNull();
      assertThat(capturedDto.places()).isEmpty();
    }

    @Test
    @DisplayName("WebSocket 전송 실패 시 에러 메시지를 클라이언트에게 전송한다")
    void shouldSendErrorMessageWhenWebSocketFails() {
      // given
      Long chatRoomId = 2L;
      Long tripPlanId = 20L;
      String userId = "user456@example.com";
      String content = "부산 여행 일정입니다.";
      AiStreamChunkReceivedEvent event =
          new AiStreamChunkReceivedEvent(
              chatRoomId, tripPlanId, userId, content, AiResponseContentType.CHAT, false);

      String expectedDestination = "/sub/chat/" + chatRoomId;
      String expectedErrorDestination = "/sub/errors/" + chatRoomId;

      willThrow(new RuntimeException("WebSocket 연결 실패"))
          .given(messagingTemplate)
          .convertAndSendToUser(eq(userId), eq(expectedDestination), any(ChatStreamDto.class));

      // when
      eventListener.handleAiStreamChunkReceived(event);

      // then
      then(messagingTemplate)
          .should()
          .convertAndSendToUser(eq(userId), eq(expectedDestination), any(ChatStreamDto.class));
      then(messagingTemplate)
          .should()
          .convertAndSendToUser(eq(userId), eq(expectedErrorDestination), any());
    }

    @Test
    @DisplayName("스트리밍 청크를 순차적으로 전송한다")
    void shouldSendStreamingChunksSequentially() {
      // given
      Long chatRoomId = 1L;
      Long tripPlanId = 10L;
      String userId = "user123@example.com";
      String expectedDestination = "/sub/chat/" + chatRoomId;

      AiStreamChunkReceivedEvent chunk1 =
          new AiStreamChunkReceivedEvent(
              chatRoomId, tripPlanId, userId, "서울", AiResponseContentType.CHAT, false);
      AiStreamChunkReceivedEvent chunk2 =
          new AiStreamChunkReceivedEvent(
              chatRoomId, tripPlanId, userId, " 3박", AiResponseContentType.CHAT, false);
      AiStreamChunkReceivedEvent chunk3 =
          new AiStreamChunkReceivedEvent(
              chatRoomId, tripPlanId, userId, " 4일 여행", AiResponseContentType.CHAT, false);
      AiStreamChunkReceivedEvent complete =
          new AiStreamChunkReceivedEvent(
              chatRoomId, tripPlanId, userId, " 일정입니다.", AiResponseContentType.CHAT, true);

      // when
      eventListener.handleAiStreamChunkReceived(chunk1);
      eventListener.handleAiStreamChunkReceived(chunk2);
      eventListener.handleAiStreamChunkReceived(chunk3);
      eventListener.handleAiStreamChunkReceived(complete);

      // then
      then(messagingTemplate)
          .should(times(4))
          .convertAndSendToUser(eq(userId), eq(expectedDestination), chatDtoCaptor.capture());

      var capturedDtos = chatDtoCaptor.getAllValues();
      assertThat(capturedDtos).hasSize(4);
      assertThat(capturedDtos.get(0).message()).isEqualTo("서울");
      assertThat(capturedDtos.get(0).isComplete()).isFalse();
      assertThat(capturedDtos.get(1).message()).isEqualTo(" 3박");
      assertThat(capturedDtos.get(1).isComplete()).isFalse();
      assertThat(capturedDtos.get(2).message()).isEqualTo(" 4일 여행");
      assertThat(capturedDtos.get(2).isComplete()).isFalse();
      assertThat(capturedDtos.get(3).message()).isEqualTo(" 일정입니다.");
      assertThat(capturedDtos.get(3).isComplete()).isTrue();
    }

    @Test
    @DisplayName("여러 사용자의 응답을 독립적으로 처리한다")
    void shouldHandleMultipleUsersIndependently() {
      // given
      AiStreamChunkReceivedEvent event1 =
          new AiStreamChunkReceivedEvent(
              1L, 10L, "user1@example.com", "첫 번째 응답", AiResponseContentType.CHAT, false);
      AiStreamChunkReceivedEvent event2 =
          new AiStreamChunkReceivedEvent(
              2L, 20L, "user2@example.com", "두 번째 응답", AiResponseContentType.CHAT, false);

      // when
      eventListener.handleAiStreamChunkReceived(event1);
      eventListener.handleAiStreamChunkReceived(event2);

      // then
      then(messagingTemplate)
          .should(times(2))
          .convertAndSendToUser(any(String.class), any(String.class), chatDtoCaptor.capture());

      var capturedDtos = chatDtoCaptor.getAllValues();
      assertThat(capturedDtos).hasSize(2);
      assertThat(capturedDtos.get(0).message()).isEqualTo("첫 번째 응답");
      assertThat(capturedDtos.get(1).message()).isEqualTo("두 번째 응답");
    }
  }
}
