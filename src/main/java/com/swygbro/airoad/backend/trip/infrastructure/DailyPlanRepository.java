package com.swygbro.airoad.backend.trip.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swygbro.airoad.backend.trip.domain.entity.DailyPlan;

/**
 * DailyPlan 엔티티에 대한 Repository 인터페이스
 *
 * <p>일일 여행 계획 데이터에 대한 데이터베이스 접근을 담당합니다.
 */
public interface DailyPlanRepository extends JpaRepository<DailyPlan, Long> {

  /**
   * 여행 계획 ID와 일차 번호로 일일 계획을 조회합니다.
   *
   * @param tripPlanId 여행 계획 ID
   * @param dayNumber 일차 번호
   * @return 일일 계획 Optional
   */
  Optional<DailyPlan> findByTripPlanIdAndDayNumber(Long tripPlanId, Integer dayNumber);

  /**
   * 여행 계획 ID로 모든 일일 계획을 날짜 순으로 조회합니다.
   *
   * @param tripPlanId 여행 계획 ID
   * @return 일일 계획 목록
   */
  @Query("SELECT dp FROM DailyPlan dp WHERE dp.tripPlan.id = :tripPlanId ORDER BY dp.date")
  List<DailyPlan> findAllByTripPlanIdOrderByDate(@Param("tripPlanId") Long tripPlanId);

  /**
   * 여행 계획 ID로 모든 일일 계획을 방문 장소와 함께 조회합니다 (N+1 방지).
   *
   * @param tripPlanId 여행 계획 ID
   * @return 일일 계획 목록 (with ScheduledPlaces)
   */
  @Query(
      "SELECT DISTINCT dp FROM DailyPlan dp "
          + "LEFT JOIN FETCH dp.scheduledPlaces "
          + "WHERE dp.tripPlan.id = :tripPlanId "
          + "ORDER BY dp.date")
  List<DailyPlan> findAllByTripPlanIdWithScheduledPlaces(@Param("tripPlanId") Long tripPlanId);
}
