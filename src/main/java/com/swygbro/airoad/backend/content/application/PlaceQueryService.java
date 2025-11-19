package com.swygbro.airoad.backend.content.application;

import org.springframework.stereotype.Service;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.domain.dto.response.PlaceResponse;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlaceQueryService implements PlaceQueryUseCase {

  private final PlaceRepository placeRepository;

  @Override
  public PlaceResponse findPlaceByName(String name) {
    Place place =
        placeRepository
            .findByName(name)
            .orElseThrow(() -> new BusinessException(TripErrorCode.PLACE_NOT_FOUND));

    return PlaceResponse.of(place);
  }
}
