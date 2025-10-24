package com.swygbro.airoad.backend.content.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.stream.Stream;

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
}
