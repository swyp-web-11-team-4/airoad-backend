package com.swygbro.airoad.backend.ai.agent.common.orchestrator;

import java.util.List;

import com.swygbro.airoad.backend.ai.agent.common.context.WorkerContext;
import com.swygbro.airoad.backend.ai.agent.common.dto.WorkerTask;
import com.swygbro.airoad.backend.ai.agent.common.worker.Worker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Orchestrator<C extends WorkerContext, E extends Enum<E>> {

  protected final List<Worker<C, E>> workers;

  protected Orchestrator(List<Worker<C, E>> workers) {
    this.workers = workers;
  }

  public abstract List<WorkerTask<E>> planTasks(C context);

  public C executeTasks(List<WorkerTask<E>> workerTasks, C context) {
    for (WorkerTask<E> workerTask : workerTasks) {
      Worker<C, E> worker = findWorker(workerTask.type());
      log.info("Worker 실행: [{}] {}", workerTask.type(), workerTask.description());
      worker.execute(context);
    }
    return context;
  }

  protected Worker<C, E> findWorker(E type) {
    return workers.stream()
        .filter(w -> w.getTaskType() == type)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("지원되지 않는 Worker: " + type));
  }
}
