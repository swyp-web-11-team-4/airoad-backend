package com.swygbro.airoad.backend.content.application;

import java.time.LocalDateTime;

/**
 * Place 임베딩 UseCase 인터페이스
 *
 * <p>Place 데이터를 벡터 스토어에 임베딩하는 비즈니스 로직을 정의합니다.
 */
public interface PlaceEmbeddingUseCase {

  /**
   * 모든 Place 데이터를 임베딩하여 벡터 스토어에 저장
   *
   * <p>Content hash 기반 Smart Upsert 방식으로 처리합니다. 내용이 변경되지 않은 경우 재임베딩을 스킵합니다.
   */
  void embedAllPlaces();

  /**
   * 특정 시점 이후 수정된 Place만 임베딩하여 벡터 스토어에 저장
   *
   * <p>증분 업데이트를 위한 메서드로, 최근 수정된 Place만 처리합니다.
   *
   * @param since 기준 시각 (이 시각 이후 수정된 Place만 처리)
   */
  void embedModifiedPlaces(LocalDateTime since);

  /**
   * 특정 Place를 임베딩하여 벡터 스토어에 저장
   *
   * <p>기존 임베딩이 있는 경우 content hash를 비교하여 변경된 경우에만 재임베딩합니다.
   *
   * @param placeId 임베딩할 Place의 ID
   */
  void embedPlace(Long placeId);
}
