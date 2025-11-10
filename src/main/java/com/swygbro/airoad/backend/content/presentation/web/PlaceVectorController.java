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

/**
 * Place 데이터를 임베딩 처리하여 벡터 스토어에 저장하는 API를 제공하는 컨트롤러 클래스입니다. 각 메서드는 특정 조건에 따라 Place 데이터를 임베딩 처리하는 작업을
 * 수행합니다.
 */
@RestController
@RequestMapping("/api/v1/admin/places")
@RequiredArgsConstructor
public class PlaceVectorController {

  private final PlaceEmbeddingUseCase placeEmbeddingUseCase;

  /**
   * 장소(Place) 데이터를 임베딩 처리하여 벡터 스토어에 저장하는 요청을 처리합니다. 'since' 파라미터가 제공되면, 지정된 시간 이후에 수정된 장소만 처리합니다.
   * 파라미터가 없으면 모든 장소를 처리합니다.
   *
   * @param since 특정 시간 이후에 수정된 장소를 대상으로 할 때 사용하는 날짜와 시간 (ISO 8601 형식)
   * @return HTTP 204 No Content 상태를 반환합니다.
   */
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

  /**
   * 지정된 장소(ID)에 대해 임베딩을 생성하고 이를 벡터 스토어에 저장하는 요청을 처리합니다. 내부적으로 지정된 Place 데이터의 임베딩을 생성하며, 기존 임베딩이 있는
   * 경우 콘텐츠 변경 여부를 확인하여 필요 시 재임베딩을 수행합니다.
   *
   * @param placeId 임베딩을 생성할 Place의 ID
   * @return HTTP 204 No Content 상태를 반환합니다.
   */
  @PostMapping("/{placeId}/vectors")
  public ResponseEntity<CommonResponse<?>> createPlaceVector(@PathVariable Long placeId) {
    placeEmbeddingUseCase.embedPlace(placeId);
    return ResponseEntity.noContent().build();
  }
}
