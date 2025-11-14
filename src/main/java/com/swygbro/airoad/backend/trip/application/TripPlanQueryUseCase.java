package com.swygbro.airoad.backend.trip.application;

import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanDetailsResponse;

public interface TripPlanQueryUseCase {
  TripPlanDetailsResponse findTripPlanDetailsById(Long tripPlanId, String username);
}
