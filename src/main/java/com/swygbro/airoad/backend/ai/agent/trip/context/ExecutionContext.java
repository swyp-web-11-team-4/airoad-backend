package com.swygbro.airoad.backend.ai.agent.trip.context;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * 파이프라인 실행 중 상태를 관리하는 컨텍스트 Enum 기반 Map으로 타입 안전한 상태 관리를 제공합니다.
 *
 * <p>각 스텝에서 필요한 데이터를 저장하고 조회하는 역할을 수행합니다. 제네릭을 활용하여 타입 안전성을 보장합니다.
 */
@Slf4j
public class ExecutionContext {

  private final Map<TripPlanContextKey, Object> state;

  public ExecutionContext() {
    this.state = new HashMap<>();
  }

  /**
   * 상태에 값을 저장하고 ExecutionContext를 반환 (메서드 체이닝용)
   *
   * @param key 상태 키
   * @param value 저장할 값
   * @param <T> 값의 타입
   * @return 현재 ExecutionContext (메서드 체이닝용)
   */
  public <T> ExecutionContext put(TripPlanContextKey key, T value) {
    state.put(key, value);
    log.debug("Context에 {}를 저장했습니다", key.getDescription());
    return this;
  }

  /**
   * 상태에서 값을 조회
   *
   * @param key 상태 키
   * @param <T> 반환할 값의 타입
   * @return 저장된 값 (없으면 null)
   */
  @SuppressWarnings("unchecked")
  public <T> T get(TripPlanContextKey key) {
    Object value = state.get(key);
    if (value == null) {
      log.warn("Context에서 {}를 찾을 수 없습니다", key.getDescription());
    }
    return (T) value;
  }
}
