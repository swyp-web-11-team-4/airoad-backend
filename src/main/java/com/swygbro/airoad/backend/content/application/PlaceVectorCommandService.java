package com.swygbro.airoad.backend.content.application;

import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.domain.converter.PlaceDocumentConverter;
import com.swygbro.airoad.backend.content.domain.dto.request.PlaceVectorSaveRequest;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceVectorStoreRepository;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceVectorCommandService implements PlaceVectorCommandUseCase {

  private final PlaceVectorStoreRepository vectorStoreRepository;
  private final PlaceRepository placeRepository;
  private final PlaceDocumentConverter placeDocumentConverter;

  @Override
  @Transactional
  public void savePlaceVector(PlaceVectorSaveRequest request) {
    log.info("VectorStore 저장 시작 - placeId: {}", request.placeId());

    try {
      vectorStoreRepository.deleteByPlaceId(request.placeId());
      log.debug("기존 임베딩 삭제 완료 - placeId: {}", request.placeId());

      Place place =
          placeRepository
              .findById(request.placeId())
              .orElseThrow(() -> new BusinessException(TripErrorCode.PLACE_NOT_FOUND));

      Double latitude = place.getLocation().getPoint().getY();
      Double longitude = place.getLocation().getPoint().getX();

      Map<String, Object> metadata =
          placeDocumentConverter.buildMetadata(
              request.placeId(),
              request.name(),
              request.address(),
              request.themes(),
              latitude,
              longitude);

      Document document = new Document(request.content(), metadata);
      vectorStoreRepository.save(document);

      log.info(
          "VectorStore에 저장 완료 - placeId: {}, documentId: {}", request.placeId(), document.getId());

    } catch (Exception e) {
      log.error("VectorStore 저장 실패 - placeId: {}", request.placeId(), e);
      throw new RuntimeException("Failed to save document to VectorStore", e);
    }
  }
}
