package com.swygbro.airoad.backend.trip.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swygbro.airoad.backend.trip.domain.entity.DailyPlan;

@Repository
public interface DailyPlanRepository extends JpaRepository<DailyPlan, Long> {

  @Modifying
  @Query("DELETE FROM DailyPlan dp WHERE dp.tripPlan.id = :tripPlanId")
  void deleteByTripPlanId(@Param("tripPlanId") Long tripPlanId);

  @Query(
      "SELECT DISTINCT dp FROM DailyPlan dp LEFT JOIN FETCH dp.scheduledPlaces as sp WHERE dp.tripPlan.id = :tripPlanId ORDER BY sp.startTime asc")
  List<DailyPlan> findAllByTripPlanId(@Param("tripPlanId") Long tripPlanId);
}
