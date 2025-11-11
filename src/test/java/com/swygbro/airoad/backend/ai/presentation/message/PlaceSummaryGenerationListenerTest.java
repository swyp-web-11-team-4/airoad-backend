package com.swygbro.airoad.backend.ai.presentation.message;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.ai.agent.summary.dto.request.AiPlaceSummaryRequest;
import com.swygbro.airoad.backend.ai.application.AiUseCase;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.content.domain.event.PlaceSummaryRequestedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PlaceSummaryGenerationListenerTest {

  @Mock private AiUseCase aiUseCase;

  @InjectMocks private PlaceSummaryGenerationListener listener;

  @Nested
  @DisplayName("장소 요약 생성 요청 이벤트 수신 시")
  class OnPlaceSummaryRequested {

    @Test
    @DisplayName("이벤트 정보를 기반으로 AI 에이전트를 호출한다")
    void givenEvent_whenReceived_thenCallAiAgent() {
      // given: 장소 요약 요청 이벤트
      PlaceSummaryRequestedEvent event =
          PlaceSummaryRequestedEvent.builder()
              .placeId(1L)
              .name("서울역")
              .address("서울특별시 용산구")
              .description("서울의 중심 역")
              .themes(List.of("관광지", "교통"))
              .build();

      // when: 이벤트 수신
      listener.onPlaceSummaryRequested(event);

      // then: placeSummaryAgent가 호출됨
      ArgumentCaptor<AiPlaceSummaryRequest> requestCaptor =
          ArgumentCaptor.forClass(AiPlaceSummaryRequest.class);
      then(aiUseCase)
          .should(times(1))
          .agentCall(eq(AgentType.PLACE_SUMMARY_AGENT), requestCaptor.capture());

      // then: 이벤트 정보가 요청에 매핑됨
      AiPlaceSummaryRequest capturedRequest = requestCaptor.getValue();
      assertThat(capturedRequest.placeId()).isEqualTo(1L);
      assertThat(capturedRequest.name()).isEqualTo("서울역");
      assertThat(capturedRequest.address()).isEqualTo("서울특별시 용산구");
      assertThat(capturedRequest.description()).isEqualTo("서울의 중심 역");
      assertThat(capturedRequest.themes()).containsExactly("관광지", "교통");
    }

    @Test
    @DisplayName("설명이 없는 경우에도 요청을 처리한다")
    void givenEventWithoutDescription_whenReceived_thenCallAiAgent() {
      // given: 설명이 없는 장소 요약 요청 이벤트
      PlaceSummaryRequestedEvent event =
          PlaceSummaryRequestedEvent.builder()
              .placeId(2L)
              .name("강남역")
              .address("서울특별시 강남구")
              .description(null)
              .themes(List.of("교통"))
              .build();

      // when: 이벤트 수신
      listener.onPlaceSummaryRequested(event);

      // then: placeSummaryAgent가 호출됨
      ArgumentCaptor<AiPlaceSummaryRequest> requestCaptor =
          ArgumentCaptor.forClass(AiPlaceSummaryRequest.class);
      then(aiUseCase)
          .should(times(1))
          .agentCall(eq(AgentType.PLACE_SUMMARY_AGENT), requestCaptor.capture());

      // then: 설명이 null로 전달됨
      AiPlaceSummaryRequest capturedRequest = requestCaptor.getValue();
      assertThat(capturedRequest.description()).isNull();
    }
  }
}
