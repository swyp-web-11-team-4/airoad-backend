package com.swygbro.airoad.backend.ai.agent.common.optimizer;

import com.swygbro.airoad.backend.ai.agent.common.context.WorkerContext;
import com.swygbro.airoad.backend.ai.agent.common.evaluator.Evaluator.EvaluationResult;

public interface Optimizer<C extends WorkerContext, E extends Enum<E>> {

  OptimizationPlan optimize(EvaluationResult<E> result, C context);

  record OptimizationPlan(String strategy, String recommendation) {}
}
