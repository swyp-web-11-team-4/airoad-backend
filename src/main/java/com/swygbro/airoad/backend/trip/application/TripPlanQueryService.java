package com.swygbro.airoad.backend.trip.application;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.ScheduledPlaceResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanDetailsResponse;
import com.swygbro.airoad.backend.trip.domain.entity.DailyPlan;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledPlace;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripPlanQueryService implements TripPlanQueryUseCase {

  private final TripPlanRepository tripPlanRepository;

  @Override
  public TripPlanDetailsResponse findTripPlanDetailsById(Long tripPlanId, String username) {
    TripPlan tripPlan =
        tripPlanRepository
            .findByIdWithDetails(tripPlanId)
            .orElseThrow(() -> new BusinessException(TripErrorCode.TRIP_PLAN_NOT_FOUND));

    if (!tripPlan.getMember().getEmail().equals(username)) {
      throw new BusinessException(TripErrorCode.TRIP_PLAN_FORBIDDEN);
    }

    return convertToResponse(tripPlan);
  }

  private TripPlanDetailsResponse convertToResponse(TripPlan tripPlan) {
    return TripPlanDetailsResponse.builder()
        .id(tripPlan.getId())
        .title(tripPlan.getTitle())
        .startDate(tripPlan.getStartDate())
        .endDate(tripPlan.getEndDate())
        .dailyPlans(
            tripPlan.getDailyPlans().stream()
                .sorted(Comparator.comparing(DailyPlan::getDayNumber))
                .map(
                    dailyPlan -> {
                      // Manually create DailyPlanResponse to handle sorting of scheduled places
                      List<ScheduledPlaceResponse> scheduledPlaceResponses =
                          dailyPlan.getScheduledPlaces().stream()
                              .sorted(Comparator.comparing(ScheduledPlace::getVisitOrder))
                              .map(ScheduledPlaceResponse::of)
                              .collect(Collectors.toList());

                      return DailyPlanResponse.builder()
                          .id(dailyPlan.getId())
                          .dayNumber(dailyPlan.getDayNumber())
                          .date(dailyPlan.getDate().toString())
                          .title(dailyPlan.getTitle())
                          .description(dailyPlan.getDescription())
                          .scheduledPlaces(scheduledPlaceResponses)
                          .build();
                    })
                .collect(Collectors.toList()))
        .build();
  }
}
