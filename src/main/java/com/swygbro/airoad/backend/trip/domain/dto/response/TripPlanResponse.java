package com.swygbro.airoad.backend.trip.domain.dto.response;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record TripPlanResponse(
    Long id,
    String title,
    LocalDate startDate,
    String region,
    String imageUrl
) {

}
