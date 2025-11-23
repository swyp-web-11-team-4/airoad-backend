package com.swygbro.airoad.backend.ai.agent.common.worker;

import com.swygbro.airoad.backend.ai.agent.common.context.WorkerContext;

public interface Worker<C extends WorkerContext, E extends Enum<E>> {

  E getTaskType();

  void execute(C context);
}
