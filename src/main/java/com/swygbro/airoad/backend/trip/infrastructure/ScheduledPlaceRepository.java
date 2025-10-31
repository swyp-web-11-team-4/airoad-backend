package com.swygbro.airoad.backend.trip.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swygbro.airoad.backend.trip.domain.entity.ScheduledPlace;

/**
 * ScheduledPlace 엔티티에 대한 Repository 인터페이스
 *
 * <p>예정된 장소 데이터에 대한 데이터베이스 접근을 담당합니다.
 */
public interface ScheduledPlaceRepository extends JpaRepository<ScheduledPlace, Long> {

  /**
   * 일일 계획 ID와 방문 순서로 예정된 장소를 조회합니다.
   *
   * @param dailyPlanId 일일 계획 ID
   * @param visitOrder 방문 순서
   * @return 예정된 장소 Optional
   */
  @Query(
      "SELECT sp FROM ScheduledPlace sp "
          + "WHERE sp.dailyPlan.id = :dailyPlanId "
          + "AND sp.visitOrder = :visitOrder")
  Optional<ScheduledPlace> findByDailyPlanIdAndVisitOrder(
      @Param("dailyPlanId") Long dailyPlanId, @Param("visitOrder") Integer visitOrder);
}
