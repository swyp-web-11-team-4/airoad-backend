package com.swygbro.airoad.backend.ai.common.context;

import java.util.List;

import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor.MetadataEntry;

/**
 * AI 프롬프트에 주입될 컨텍스트를 제공하는 인터페이스
 *
 * <p>각 구현체는 특정 타입의 데이터로부터 컨텍스트 메타데이터를 생성합니다.
 *
 * @param <T> 처리할 데이터 타입
 */
public interface ContextProvider<T> {

  /**
   * 주어진 데이터로부터 컨텍스트 메타데이터를 생성합니다.
   *
   * @param data 컨텍스트 생성에 필요한 데이터
   * @return 생성된 메타데이터 엔트리 리스트
   */
  List<MetadataEntry> getContext(Object data);

  /**
   * 이 Provider가 지원하는 데이터 타입을 반환합니다.
   *
   * @return 지원하는 데이터 타입의 Class 객체
   */
  Class<T> getSupportedType();

  /**
   * 컨텍스트 주입 순서를 결정합니다. 낮을수록 먼저 실행됩니다.
   *
   * @return 순서 값 (음수일수록 우선순위 높음)
   */
  int getOrder();
}
