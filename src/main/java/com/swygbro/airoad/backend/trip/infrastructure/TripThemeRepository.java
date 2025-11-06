package com.swygbro.airoad.backend.trip.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swygbro.airoad.backend.trip.domain.entity.TripTheme;

@Repository
public interface TripThemeRepository extends JpaRepository<TripTheme, Long> {

  @Modifying
  @Query("DELETE FROM TripTheme tt WHERE tt.tripPlan.id = :tripPlanId")
  void deleteByTripPlanId(@Param("tripPlanId") Long tripPlanId);
}
