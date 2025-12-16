package com.swygbro.airoad.backend.trip.application;

import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceUpdateRequest;

public interface ScheduledPlaceCommandUseCase {
  void saveScheduledPlace(
      Long chatRoomId,
      Long tripPlanId,
      String username,
      Integer dayNumber,
      ScheduledPlaceCreateRequest request);

  void updateScheduledPlace(
      Long chatRoomId,
      Long tripPlanId,
      String username,
      Integer dayNumber,
      Integer visitOrder,
      ScheduledPlaceUpdateRequest request);

  void replaceScheduledPlace(
      Long chatRoomId,
      Long tripPlanId,
      String username,
      Integer dayNumber,
      Integer visitOrder,
      Long placeId,
      String summary);

  void deleteScheduledPlace(
      Long chatRoomId, Long tripPlanId, String username, Integer dayNumber, Integer visitOrder);

  void swapScheduledPlaces(
      Long chatRoomId,
      Long tripPlanId,
      String username,
      Integer dayNumber,
      Integer visitOrderA,
      Integer visitOrderB);
}
