package com.swygbro.airoad.backend.trip.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;

public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {

  @Query(
      "SELECT t FROM TripPlan t WHERE t.member.id = :memberId AND (:cursor IS NULL OR t.id < :cursor)")
  Slice<TripPlan> findByMemberIdWithCursor(
      @Param("memberId") Long memberId, @Param("cursor") Long cursor, Pageable pageable);

  boolean existsByIdAndMemberId(Long id, Long memberId);
}
