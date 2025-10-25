package com.swygbro.airoad.backend.common.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 처리 설정
 *
 * <p>Spring의 @Async 어노테이션을 활성화하고, 비동기 작업 실행을 위한 스레드 풀을 구성합니다.
 *
 * <h3>주요 용도</h3>
 *
 * <ul>
 *   <li>AI 응답 WebSocket 전송 비동기 처리
 *   <li>이벤트 리스너 비동기 실행
 *   <li>메시지 저장 로직과 WebSocket 전송 분리
 * </ul>
 *
 * <h3>스레드 풀 설정</h3>
 *
 * <ul>
 *   <li><strong>corePoolSize</strong>: 2 - 기본 스레드 수
 *   <li><strong>maxPoolSize</strong>: 5 - 최대 스레드 수
 *   <li><strong>queueCapacity</strong>: 100 - 대기 큐 크기
 * </ul>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

  /**
   * 비동기 작업 실행을 위한 스레드 풀 TaskExecutor를 생성합니다.
   *
   * @return 설정된 ThreadPoolTaskExecutor
   */
  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("async-");
    executor.initialize();
    return executor;
  }
}
