package com.swygbro.airoad.backend.trip.application;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledPlace;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;
import com.swygbro.airoad.backend.trip.infrastructure.DailyPlanRepository;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DailyPlanQueryService implements DailyPlanQueryUseCase {
  private final TripPlanRepository tripPlanRepository;
  private final DailyPlanRepository dailyPlanRepository;

  @Override
  @Transactional(readOnly = true)
  public List<DailyPlanResponse> getDailyPlanListByTripPlanId(Long tripPlanId, Long memberId) {
    TripPlan tripPlan =
        tripPlanRepository
            .findByIdWithMember(tripPlanId)
            .orElseThrow(() -> new BusinessException(TripErrorCode.TRIP_PLAN_NOT_FOUND));
    if (!tripPlan.getMember().getId().equals(memberId)) {
      throw new BusinessException(TripErrorCode.TRIP_PLAN_FORBIDDEN);
    }
    return dailyPlanRepository.findAllByTripPlanId(tripPlanId).stream()
        .map(DailyPlanResponse::of)
        .toList();
  }
}
