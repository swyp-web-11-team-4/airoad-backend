package com.swygbro.airoad.backend.trip.application;

import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceUpdateRequest;

public interface ScheduledPlaceCommandUseCase {
  void saveScheduledPlace(
      String username, Long tripPlanId, Integer dayNumber, ScheduledPlaceCreateRequest request);

  void updateScheduledPlace(
      String username,
      Long tripPlanId,
      Integer dayNumber,
      Integer visitOrder,
      ScheduledPlaceUpdateRequest request);

  void deleteScheduledPlace(
      String username, Long tripPlanId, Integer dayNumber, Integer visitOrder);

  boolean validateScheduledPlace(String username, Long scheduledPlaceId);
}
