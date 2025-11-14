package com.swygbro.airoad.backend.ai.common.context;

import java.util.List;

import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor.MetadataEntry;

/**
 * ContextProvider의 추상 구현체로, 타입 체크 및 공통 로직을 제공합니다.
 *
 * <p>하위 클래스는 {@link #doGetContext(Object)} 메서드만 구현하면 됩니다.
 *
 * @param <T> 처리할 데이터 타입
 */
public abstract class AbstractContextProvider<T> implements ContextProvider<T> {

  private final Class<T> supportedType;

  /**
   * 지원하는 데이터 타입을 지정하여 Provider를 생성합니다.
   *
   * @param supportedType 이 Provider가 처리할 데이터 타입
   */
  protected AbstractContextProvider(Class<T> supportedType) {
    this.supportedType = supportedType;
  }

  @Override
  public Class<T> getSupportedType() {
    return supportedType;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<MetadataEntry> getContext(Object data) {
    return doGetContext((T) data);
  }

  /**
   * 실제 컨텍스트 생성 로직을 구현합니다.
   *
   * <p>이 메서드는 타입이 보장된 상태에서 호출되므로, 타입 체크 없이 안전하게 사용할 수 있습니다.
   *
   * @param data 타입이 보장된 데이터
   * @return 생성된 메타데이터 엔트리 리스트
   */
  protected abstract List<MetadataEntry> doGetContext(T data);
}
