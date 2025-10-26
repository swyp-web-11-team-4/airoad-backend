package com.swygbro.airoad.backend.common.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 처리 및 재시도 설정
 *
 * <p>Spring의 @Async 및 @Retryable 어노테이션을 활성화하고, 비동기 작업 실행을 위한 스레드 풀을 구성합니다.
 *
 * <h3>주요 용도</h3>
 *
 * <ul>
 *   <li>AI 응답 WebSocket 전송 비동기 처리
 *   <li>이벤트 리스너 비동기 실행
 *   <li>메시지 저장 로직과 WebSocket 전송 분리
 *   <li>DB 저장 실패 시 자동 재시도
 * </ul>
 *
 * <h3>스레드 풀 설정</h3>
 *
 * <ul>
 *   <li><strong>corePoolSize</strong>: 5 - 기본 스레드 수 (동시 AI 응답 처리용)
 *   <li><strong>maxPoolSize</strong>: 10 - 최대 스레드 수 (피크 타임 대응)
 *   <li><strong>queueCapacity</strong>: 50 - 대기 큐 크기 (과도한 적체 방지)
 *   <li><strong>rejectedExecutionHandler</strong>: CallerRunsPolicy - 큐 포화 시 호출 스레드에서 직접 실행
 * </ul>
 *
 * <h3>크기 선정 근거</h3>
 *
 * <p>I/O-bound 작업(WebSocket 전송, DB 저장) 특성상:
 *
 * <ul>
 *   <li>Core 5개: 일반적인 부하 시 충분한 동시 처리 능력
 *   <li>Max 10개: 피크 타임에도 대응 가능하되 과도한 리소스 사용 방지
 *   <li>Queue 50개: 일시적 급증 흡수, 너무 크면 메모리 낭비 및 지연 증가
 * </ul>
 *
 * <h3>작업 거부 정책 (CallerRunsPolicy)</h3>
 *
 * <p>스레드 풀과 큐가 모두 가득 찬 경우:
 *
 * <ul>
 *   <li>작업을 거부하지 않고 호출한 스레드(caller)가 직접 실행
 *   <li>AI 응답 전송과 같은 중요한 작업의 손실 방지
 *   <li>자연스러운 백프레셔(back-pressure) 제공으로 시스템 과부하 방지
 * </ul>
 */
@Configuration
@EnableAsync
@EnableRetry
public class AsyncConfig {

  /**
   * 비동기 작업 실행을 위한 스레드 풀 TaskExecutor를 생성합니다.
   *
   * <p>CallerRunsPolicy를 통해 스레드 풀 포화 시에도 작업이 손실되지 않도록 보장합니다.
   *
   * @return 설정된 ThreadPoolTaskExecutor
   */
  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("async-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }
}
