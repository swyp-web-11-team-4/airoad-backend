package com.swygbro.airoad.backend.ai.application;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.ai.agent.AiroadAgent;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AiServiceTest {

  @Mock private AiroadAgent mockAgent1;

  @Mock private AiroadAgent mockAgent2;

  @InjectMocks private AiService aiService;

  @Nested
  @DisplayName("agentCall 메서드는")
  class AgentCall {

    @Test
    @DisplayName("올바른 에이전트 이름으로 호출하면 해당 에이전트를 실행한다")
    void shouldExecuteAgentWithCorrectName() {
      // given
      String agentName = "testAgent";
      Object request = new Object();
      List<AiroadAgent> agents = List.of(mockAgent1, mockAgent2);

      given(mockAgent1.supports(agentName)).willReturn(true);

      aiService = new AiService(agents);

      // when
      aiService.agentCall(agentName, request);

      // then
      verify(mockAgent1).execute(request);
      verify(mockAgent2, never()).execute(any());
    }

    @Test
    @DisplayName("존재하지 않는 에이전트 이름으로 호출하면 AGENT_NOT_FOUND 예외를 발생시킨다")
    void shouldThrowExceptionWhenAgentNotFound() {
      // given
      String nonExistentAgentName = "nonExistentAgent";
      Object request = new Object();
      List<AiroadAgent> agents = List.of(mockAgent1, mockAgent2);

      given(mockAgent1.supports(nonExistentAgentName)).willReturn(false);
      given(mockAgent2.supports(nonExistentAgentName)).willReturn(false);

      aiService = new AiService(agents);

      // when & then
      assertThatThrownBy(() -> aiService.agentCall(nonExistentAgentName, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", AiErrorCode.AGENT_NOT_FOUND);

      verify(mockAgent1, never()).execute(any());
      verify(mockAgent2, never()).execute(any());
    }

    @Test
    @DisplayName("여러 에이전트 중 이름이 일치하는 첫 번째 에이전트를 실행한다")
    void shouldExecuteFirstMatchingAgent() {
      // given
      String agentName = "targetAgent";
      Object request = new Object();
      AiroadAgent mockAgent3 = mock(AiroadAgent.class);
      List<AiroadAgent> agents = List.of(mockAgent1, mockAgent2, mockAgent3);

      given(mockAgent1.supports(agentName)).willReturn(false);
      given(mockAgent2.supports(agentName)).willReturn(true);

      aiService = new AiService(agents);

      // when
      aiService.agentCall(agentName, request);

      // then
      verify(mockAgent1, never()).execute(any());
      verify(mockAgent2).execute(request);
      verify(mockAgent3, never()).execute(any()); // 첫 번째 매칭 후 중단
    }

    @Test
    @DisplayName("에이전트 리스트가 비어있으면 AGENT_NOT_FOUND 예외를 발생시킨다")
    void shouldThrowExceptionWhenAgentListIsEmpty() {
      // given
      String agentName = "testAgent";
      Object request = new Object();
      List<AiroadAgent> emptyAgents = List.of();

      aiService = new AiService(emptyAgents);

      // when & then
      assertThatThrownBy(() -> aiService.agentCall(agentName, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", AiErrorCode.AGENT_NOT_FOUND);
    }

    @Test
    @DisplayName("요청 객체를 에이전트에 정확히 전달한다")
    void shouldPassRequestObjectToAgent() {
      // given
      String agentName = "testAgent";
      String request = "test request data";
      List<AiroadAgent> agents = List.of(mockAgent1);

      given(mockAgent1.supports(agentName)).willReturn(true);

      aiService = new AiService(agents);

      // when
      aiService.agentCall(agentName, request);

      // then
      verify(mockAgent1).execute(request);
    }
  }
}
