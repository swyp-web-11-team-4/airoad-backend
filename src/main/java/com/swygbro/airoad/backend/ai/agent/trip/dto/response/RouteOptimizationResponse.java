package com.swygbro.airoad.backend.ai.agent.trip.dto.response;

import java.util.List;

import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;

import lombok.Builder;

@Builder
public record RouteOptimizationResponse(List<RoutedPlace> places) {

  @Builder
  public record RoutedPlace(Long placeId, int visitOrder, ScheduledCategory category) {}

  public static RouteOptimizationResponse of(List<RoutedPlace> places) {
    return RouteOptimizationResponse.builder().places(places).build();
  }
}
