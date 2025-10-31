package com.swygbro.airoad.backend.ai.agent.trip;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class TripAgentTest {

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private ChatModel chatModel;

  @Mock private VectorStore vectorStore;

  private TripAgent tripAgent;

  @BeforeEach
  void setUp() {
    tripAgent = new TripAgent(eventPublisher, chatModel, vectorStore);
  }

  @Nested
  @DisplayName("에이전트 지원 여부를 확인할 때")
  class SupportsTests {

    @Test
    @DisplayName("tripAgent 이름으로 요청하면 true를 반환한다")
    void tripAgent_이름으로_요청하면_true를_반환() {
      // given
      String agentName = "tripAgent";

      // when
      boolean result = tripAgent.supports(agentName);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("다른 에이전트 이름으로 요청하면 false를 반환한다")
    void 다른_에이전트_이름으로_요청하면_false를_반환() {
      // given
      String agentName = "chatAgent";

      // when
      boolean result = tripAgent.supports(agentName);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("빈 문자열로 요청하면 false를 반환한다")
    void 빈_문자열로_요청하면_false를_반환() {
      // given
      String agentName = "";

      // when
      boolean result = tripAgent.supports(agentName);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("null로 요청하면 false를 반환한다")
    void null로_요청하면_false를_반환() {
      // given
      String agentName = null;

      // when
      boolean result = tripAgent.supports(agentName);

      // then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("여행 일정 생성을 실행할 때")
  class ExecuteTests {

    @Test
    @DisplayName("요청 데이터로 AI 에이전트를 실행한다")
    void 요청_데이터로_AI_에이전트를_실행() {
      // given
      AiDailyPlanRequest request =
          AiDailyPlanRequest.builder()
              .chatRoomId(1L)
              .tripPlanId(100L)
              .themes(List.of("힐링", "맛집"))
              .startDate(LocalDate.of(2025, 12, 1))
              .duration(3)
              .region("제주")
              .peopleCount(2)
              .transportation(Transportation.PUBLIC_TRANSIT)
              .build();

      // when & then
      assertThatCode(() -> tripAgent.execute(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("복수 테마로 여행 일정을 생성한다")
    void 복수_테마로_여행_일정_생성() {
      // given
      AiDailyPlanRequest request =
          AiDailyPlanRequest.builder()
              .chatRoomId(2L)
              .tripPlanId(200L)
              .themes(List.of("문화", "역사", "맛집", "쇼핑"))
              .startDate(LocalDate.of(2025, 11, 1))
              .duration(4)
              .region("서울")
              .peopleCount(4)
              .transportation(Transportation.PUBLIC_TRANSIT)
              .build();

      // when & then
      assertThatCode(() -> tripAgent.execute(request)).doesNotThrowAnyException();
    }
  }
}
