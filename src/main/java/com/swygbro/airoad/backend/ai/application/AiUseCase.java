package com.swygbro.airoad.backend.ai.application;

import com.swygbro.airoad.backend.ai.domain.entity.AgentType;

public interface AiUseCase {

  void agentCall(AgentType agentType, Object request);
}
