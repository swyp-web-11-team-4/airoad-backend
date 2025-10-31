package com.swygbro.airoad.backend.ai.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.swygbro.airoad.backend.ai.agent.AiroadAgent;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService implements AiUseCase {

  private final List<AiroadAgent> agents;

  @Override
  public void agentCall(String agentName, Object request) {
    log.debug("{} called with request {}", agentName, request);

    agents.stream()
        .filter(it -> it.name().equals(agentName))
        .findFirst()
        .orElseThrow(() -> new BusinessException(AiErrorCode.AGENT_NOT_FOUND))
        .execute(request);
  }
}
