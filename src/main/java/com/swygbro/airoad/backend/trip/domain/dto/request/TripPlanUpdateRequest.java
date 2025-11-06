package com.swygbro.airoad.backend.trip.domain.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Builder;

@Builder
public record TripPlanUpdateRequest(@NotBlank(message = "여행 제목은 비워둘 수 없습니다.") String title) {}
