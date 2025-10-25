package com.swygbro.airoad.backend.trip.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;

/**
 * TripPlan 엔티티에 대한 Repository 인터페이스
 *
 * <p>여행 계획 데이터에 대한 데이터베이스 접근을 담당합니다.
 */
public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {}
