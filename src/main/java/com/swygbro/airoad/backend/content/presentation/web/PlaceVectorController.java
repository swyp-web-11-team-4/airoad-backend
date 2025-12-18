package com.swygbro.airoad.backend.content.presentation.web;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.content.application.PlaceEmbeddingUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/places")
@RequiredArgsConstructor
public class PlaceVectorController implements PlaceVectorApi {

  private final PlaceEmbeddingUseCase placeEmbeddingUseCase;

  @Override
  @PostMapping("/vectors")
  public ResponseEntity<CommonResponse<?>> createPlacesVector(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime since) {
    if (since != null) {
      placeEmbeddingUseCase.embedModifiedPlaces(since);
    } else {
      placeEmbeddingUseCase.embedAllPlaces();
    }
    return ResponseEntity.noContent().build();
  }

  @Override
  @PostMapping("/{placeId}/vectors")
  public ResponseEntity<CommonResponse<?>> createPlaceVector(@PathVariable Long placeId) {
    placeEmbeddingUseCase.embedPlace(placeId);
    return ResponseEntity.noContent().build();
  }
}
