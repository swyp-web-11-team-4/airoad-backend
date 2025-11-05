package com.swygbro.airoad.backend.trip.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;

public interface TripPlanRepository
    extends JpaRepository<TripPlan, Long>, JpaSpecificationExecutor<TripPlan> {

  @Query("SELECT tp FROM TripPlan tp JOIN FETCH tp.member WHERE tp.id = :tripPlanId")
  Optional<TripPlan> findByIdWithMember(@Param("tripPlanId") Long tripPlanId);

  boolean existsByIdAndMemberId(Long id, Long memberId);
}
