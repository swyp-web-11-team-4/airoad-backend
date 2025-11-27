package com.swygbro.airoad.backend.trip.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swygbro.airoad.backend.trip.domain.entity.ScheduledPlace;

@Repository
public interface ScheduledPlaceRepository extends JpaRepository<ScheduledPlace, Long> {

  @Modifying
  @Query(
      "DELETE FROM ScheduledPlace sp WHERE sp.dailyPlan.id IN (SELECT dp.id FROM DailyPlan dp WHERE dp.tripPlan.id = :tripPlanId)")
  void deleteByTripPlanId(@Param("tripPlanId") Long tripPlanId);
}
