package com.swygbro.airoad.backend.content.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.swygbro.airoad.backend.content.domain.entity.Place;

/** Place 엔티티의 JPA Repository */
public interface PlaceRepository extends JpaRepository<Place, Long> {

  /**
   * 모든 Place를 Stream으로 조회
   *
   * <p>대량의 데이터를 메모리에 한번에 로드하지 않고 스트림으로 처리하기 위해 사용합니다. 반드시 @Transactional(readOnly = true)와 함께 사용해야
   * 합니다.
   *
   * @return Place Stream
   */
  Stream<Place> streamAllBy();

  /**
   * 특정 시점 이후 수정된 Place를 Stream으로 조회
   *
   * <p>증분 업데이트를 위해 최근 수정된 Place만 조회합니다. 반드시 @Transactional(readOnly = true)와 함께 사용해야 합니다.
   *
   * @param dateTime 기준 시각
   * @return 기준 시각 이후 수정된 Place Stream
   */
  Stream<Place> streamByUpdatedAtAfter(LocalDateTime dateTime);

  /**
   * TourAPI 장소 ID로 Place 조회
   *
   * @param apiPlaceId TourAPI 장소 ID
   * @return Place Optional
   */
  Optional<Place> findByApiPlaceId(Long apiPlaceId);

  /**
   * description이 null인 Place 목록 조회 (Phase 2에서 overview 업데이트용)
   *
   * @return description이 null인 Place 목록
   */
  List<Place> findByDescriptionIsNull();

  /**
   * description이 null인 Place를 제한된 개수만큼 조회 (API 호출 제한 고려)
   *
   * @param pageable 페이지 정보 (limit 포함)
   * @return description이 null인 Place 목록 (최대 limit개)
   */
  List<Place> findByDescriptionIsNullOrderByIdAsc(
      Pageable pageable);
}
