package com.swygbro.airoad.backend.ai.common.context;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor.MetadataEntry;

import lombok.extern.slf4j.Slf4j;

/**
 * 모든 ContextProvider를 관리하고 컨텍스트를 조합하는 매니저
 *
 * <p>등록된 Provider들을 타입별로 그룹화하여 효율적으로 컨텍스트를 생성합니다.
 */
@Slf4j
@Component
public class ContextManager {

  private final Map<Class<?>, List<ContextProvider<?>>> providersByType;

  /**
   * Spring이 자동 주입한 모든 ContextProvider를 타입별로 그룹화합니다.
   *
   * @param providers 모든 ContextProvider 구현체들
   */
  public ContextManager(List<ContextProvider<?>> providers) {
    this.providersByType =
        providers.stream().collect(Collectors.groupingBy(ContextProvider::getSupportedType));

    log.info(
        "ContextManager 초기화 완료 - {} 개 타입, {} 개 Provider", providersByType.size(), providers.size());
  }

  /**
   * 주어진 데이터들로부터 컨텍스트를 구성합니다.
   *
   * <p>각 데이터에 대해 해당 타입을 지원하는 Provider를 찾아 컨텍스트를 생성하고, Order 순서대로 정렬하여 반환합니다.
   *
   * @param data 컨텍스트 생성에 필요한 데이터들 (가변 인자)
   * @return 조합된 메타데이터 엔트리 리스트
   */
  public List<MetadataEntry> buildContext(Object... data) {
    log.debug("컨텍스트 구성 시작 - {} 개 데이터", data.length);

    List<MetadataEntry> result =
        Arrays.stream(data)
            .filter(Objects::nonNull)
            .flatMap(d -> collectContext(d.getClass(), d).stream())
            .toList();

    log.debug("컨텍스트 구성 완료 - {} 개 메타데이터 엔트리", result.size());
    return result;
  }

  /**
   * 특정 타입의 데이터에 대한 컨텍스트를 수집합니다.
   *
   * @param dataType 데이터 타입
   * @param data 실제 데이터
   * @param <T> 데이터 타입 파라미터
   * @return 수집된 메타데이터 엔트리 리스트
   */
  @SuppressWarnings("unchecked")
  private <T> List<MetadataEntry> collectContext(Class<?> dataType, T data) {
    List<ContextProvider<?>> providers = providersByType.get(dataType);

    if (providers == null || providers.isEmpty()) {
      log.debug("타입 {}에 대한 Provider가 없습니다", dataType.getSimpleName());
      return List.of();
    }

    log.debug("타입 {}에 대해 {} 개 Provider 실행", dataType.getSimpleName(), providers.size());

    return providers.stream()
        .sorted(Comparator.comparingInt(ContextProvider::getOrder))
        .flatMap(
            provider -> {
              try {
                ContextProvider<T> typedProvider = (ContextProvider<T>) provider;
                List<MetadataEntry> entries = typedProvider.getContext(data);
                log.debug(
                    "  ↳ {} - {} 개 엔트리 생성", provider.getClass().getSimpleName(), entries.size());
                return entries.stream();
              } catch (Exception e) {
                log.warn(
                    "  ✗ {} - 컨텍스트 생성 실패: {}", provider.getClass().getSimpleName(), e.getMessage());
                return Stream.empty();
              }
            })
        .toList();
  }
}
