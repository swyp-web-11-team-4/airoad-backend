package com.swygbro.airoad.backend.content.domain.dto.response;

import java.util.List;

import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;

import lombok.Builder;

@Builder
public record PlaceResponse(
    Long id,
    String name,
    String address,
    Double latitude,
    Double longitude,
    String description,
    String imageUrl,
    String operatingHours,
    String holidayInfo,
    Boolean isMustVisit,
    List<PlaceThemeType> themes) {
  public static PlaceResponse of(Place place) {
    return PlaceResponse.builder()
        .id(place.getId())
        .name(place.getLocation().getName())
        .address(place.getLocation().getAddress())
        .latitude(place.getLocation().getPoint().getY())
        .longitude(place.getLocation().getPoint().getX())
        .description(place.getDescription())
        .imageUrl(place.getImageUrl())
        .operatingHours(place.getOperatingHours())
        .holidayInfo(place.getHolidayInfo())
        .isMustVisit(place.getIsMustVisit())
        .themes(place.getThemes().stream().toList())
        .build();
  }
}
