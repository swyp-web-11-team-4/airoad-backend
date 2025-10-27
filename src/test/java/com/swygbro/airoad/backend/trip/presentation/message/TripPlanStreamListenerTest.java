package com.swygbro.airoad.backend.trip.presentation.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.ai.domain.dto.AiResponseContentType;
import com.swygbro.airoad.backend.ai.domain.event.AiStreamChunkReceivedEvent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * TripPlanStreamHandler의 테스트 클래스입니다.
 *
 * <p>AI 응답 스트림을 여행 일정 데이터로 변환하는 핸들러의 기능을 검증합니다. 현재는 스켈레톤 구현으로 로깅만 수행하며, 향후 버퍼링, 파싱, DB 저장 로직이 추가될
 * 예정입니다.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("TripPlanStreamHandler 클래스")
class TripPlanStreamListenerTest {

  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private TripPlanStreamListener handler;

  private static final Long TEST_CHAT_ROOM_ID = 1L;
  private static final Long TEST_TRIP_PLAN_ID = 100L;
  private static final String TEST_USER_ID = "test@example.com";

  @Nested
  @DisplayName("handleAiStreamChunkReceived 메서드는")
  class HandleAiResponseReceived {

    @Test
    @DisplayName("CHAT 타입 응답은 무시하고 처리하지 않는다")
    void shouldIgnoreChatTypeResponse() {
      // given - CHAT 타입 이벤트
      AiStreamChunkReceivedEvent event =
          new AiStreamChunkReceivedEvent(
              TEST_CHAT_ROOM_ID,
              TEST_TRIP_PLAN_ID,
              TEST_USER_ID,
              "안녕하세요, 제주도 여행 어떠세요?",
              AiResponseContentType.CHAT,
              false);

      // when
      handler.handleAiStreamChunkReceived(event);

      // then - 이벤트 발행 없음
      verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("SCHEDULE 타입 응답을 정상 처리한다")
    void shouldHandleScheduleTypeResponse() {
      // given - SCHEDULE 타입 이벤트
      String scheduleContent = "## 1일차\n제주공항 도착 - 09:00\n성산일출봉 방문 - 11:00";
      AiStreamChunkReceivedEvent event =
          new AiStreamChunkReceivedEvent(
              TEST_CHAT_ROOM_ID,
              TEST_TRIP_PLAN_ID,
              TEST_USER_ID,
              scheduleContent,
              AiResponseContentType.SCHEDULE,
              false);

      // when & then - 예외 없이 실행됨
      assertThatCode(() -> handler.handleAiStreamChunkReceived(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("완료된 SCHEDULE 응답(isComplete=true)을 처리한다")
    void shouldHandleCompletedScheduleStream() {
      // given - 완료된 스트림
      String completedContent = "## 3일차\n제주공항 출발 - 18:00";
      AiStreamChunkReceivedEvent event =
          new AiStreamChunkReceivedEvent(
              TEST_CHAT_ROOM_ID,
              TEST_TRIP_PLAN_ID,
              TEST_USER_ID,
              completedContent,
              AiResponseContentType.SCHEDULE,
              true);

      // when & then - 예외 없이 실행됨
      assertThatCode(() -> handler.handleAiStreamChunkReceived(event)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("SCHEDULE 스트림 처리")
  class ScheduleStreamHandling {

    @Test
    @DisplayName("중간 청크를 버퍼링하며 예외 없이 처리한다")
    void shouldBufferIntermediateChunks() {
      // given - 중간 청크
      AiStreamChunkReceivedEvent chunkEvent =
          new AiStreamChunkReceivedEvent(
              TEST_CHAT_ROOM_ID,
              TEST_TRIP_PLAN_ID,
              TEST_USER_ID,
              "성산일출봉 방문",
              AiResponseContentType.SCHEDULE,
              false);

      // when & then
      assertThatCode(() -> handler.handleAiStreamChunkReceived(chunkEvent))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("긴 일정 내용도 정상 처리한다")
    void shouldHandleLongScheduleContent() {
      // given - 긴 일정 내용
      String longContent =
          "## 1일차\n"
              + "제주공항 도착 (09:00)\n"
              + "차량 렌트 (09:30)\n"
              + "성산일출봉 방문 (11:00-13:00)\n"
              + "점심식사 - 해산물 전문점 (13:30)\n"
              + "섭지코지 산책 (15:00-17:00)\n"
              + "숙소 체크인 (18:00)\n"
              + "저녁식사 - 흑돼지 구이 (19:00)";

      AiStreamChunkReceivedEvent event =
          new AiStreamChunkReceivedEvent(
              TEST_CHAT_ROOM_ID,
              TEST_TRIP_PLAN_ID,
              TEST_USER_ID,
              longContent,
              AiResponseContentType.SCHEDULE,
              false);

      // when & then
      assertThatCode(() -> handler.handleAiStreamChunkReceived(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("빈 응답 내용도 안전하게 처리한다")
    void shouldHandleEmptyContent() {
      // given - 빈 응답
      AiStreamChunkReceivedEvent event =
          new AiStreamChunkReceivedEvent(
              TEST_CHAT_ROOM_ID,
              TEST_TRIP_PLAN_ID,
              TEST_USER_ID,
              "",
              AiResponseContentType.SCHEDULE,
              false);

      // when & then
      assertThatCode(() -> handler.handleAiStreamChunkReceived(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다수의 청크를 순차적으로 처리한다")
    void shouldHandleMultipleChunksSequentially() {
      // given - 여러 청크
      AiStreamChunkReceivedEvent event1 =
          new AiStreamChunkReceivedEvent(
              TEST_CHAT_ROOM_ID,
              TEST_TRIP_PLAN_ID,
              TEST_USER_ID,
              "## 1일차\n",
              AiResponseContentType.SCHEDULE,
              false);

      AiStreamChunkReceivedEvent event2 =
          new AiStreamChunkReceivedEvent(
              TEST_CHAT_ROOM_ID,
              TEST_TRIP_PLAN_ID,
              TEST_USER_ID,
              "제주공항 도착\n",
              AiResponseContentType.SCHEDULE,
              false);

      AiStreamChunkReceivedEvent event3 =
          new AiStreamChunkReceivedEvent(
              TEST_CHAT_ROOM_ID,
              TEST_TRIP_PLAN_ID,
              TEST_USER_ID,
              "성산일출봉 방문",
              AiResponseContentType.SCHEDULE,
              false);

      // when & then - 여러 청크를 순차 처리해도 예외 없음
      assertThatCode(
              () -> {
                handler.handleAiStreamChunkReceived(event1);
                handler.handleAiStreamChunkReceived(event2);
                handler.handleAiStreamChunkReceived(event3);
              })
          .doesNotThrowAnyException();
    }
  }
}
