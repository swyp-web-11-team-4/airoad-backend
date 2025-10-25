package com.swygbro.airoad.backend.content.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.content.domain.converter.PlaceDocumentConverter;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceVectorStoreRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Place 임베딩 서비스 구현체
 *
 * <p>현재 구현: @Transactional 내에서 JPA Stream과 VectorStore 작업을 모두 수행
 *
 * <p>개선 고려사항: 이벤트 기반의 아키텍처로 전환 및 분리
 *
 * <ul>
 *   <li>Place 수정 시 도메인 이벤트 발행 → 비동기 임베딩 처리로 전환
 * </ul>
 *
 * <p>참고:
 *
 * <ul>
 *   <li>현재는 데이터 수정 빈도가 낮고 새벽 배치로 처리되므로 단순 구현 유지
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceEmbeddingService implements PlaceEmbeddingUseCase {

  /**
   * 배치 사이즈는 반드시 1로 고정해야 합니다. 네이버 CLOVA X의 경우 임베딩에 필요한 input 필드 지원을 String 형식만 지원합니다. 이번 MVP에서는 구현이
   * 우선이므로 여행지 장소 임베딩을 단건 처리로 API 요청을 보내도록 하고 추후 청킹 전략으로 전환이 필요합니다.
   */
  private static final int BATCH_SIZE = 1;

  private final PlaceRepository placeRepository;
  private final PlaceVectorStoreRepository vectorStoreRepository;
  private final PlaceDocumentConverter documentConverter;

  /**
   * 모든 Place 데이터를 임베딩하여 벡터 스토어에 저장
   *
   * <p>Stream + Batch 방식으로 메모리 효율적으로 처리하며, 기존 임베딩은 삭제 후 재생성합니다.
   */
  @Override
  @Transactional
  public void embedAllPlaces() {
    log.info("Starting to embed all places");
    try (Stream<Place> placeStream = placeRepository.streamAllBy()) {
      processPlaceStream(placeStream);
    }
  }

  /**
   * 특정 시점 이후 수정된 Place만 임베딩하여 벡터 스토어에 저장
   *
   * <p>증분 업데이트를 위해 최근 수정된 Place만 처리합니다.
   *
   * @param since 기준 시각 (이 시각 이후 수정된 Place만 처리)
   */
  @Override
  @Transactional
  public void embedModifiedPlaces(LocalDateTime since) {
    log.info("Starting to embed places modified after: {}", since);
    try (Stream<Place> placeStream = placeRepository.streamByUpdatedAtAfter(since)) {
      processPlaceStream(placeStream);
    }
  }

  /**
   * 특정 Place를 임베딩하여 벡터 스토어에 저장
   *
   * <p>기존 임베딩은 삭제 후 재생성합니다.
   *
   * @param placeId 임베딩할 Place의 ID
   * @throws IllegalArgumentException Place를 찾을 수 없는 경우
   */
  @Override
  @Transactional
  public void embedPlace(Long placeId) {
    log.info("Embedding place with ID: {}", placeId);

    Place place =
        placeRepository
            .findById(placeId)
            .orElseThrow(
                () -> {
                  log.error("Place not found with ID: {}", placeId);
                  return new IllegalArgumentException("Place not found with ID: " + placeId);
                });

    processPlace(place);
    log.info(
        "Successfully embedded place - ID: {}, Name: {}", placeId, place.getLocation().getName());
  }

  /**
   * Place Stream을 배치 단위로 처리
   *
   * @param placeStream 처리할 Place Stream
   */
  private void processPlaceStream(Stream<Place> placeStream) {
    List<Place> batch = new ArrayList<>(BATCH_SIZE);

    placeStream.forEach(
        place -> {
          batch.add(place);

          if (batch.size() >= BATCH_SIZE) {
            processBatch(new ArrayList<>(batch));
            batch.clear();
          }
        });

    if (!batch.isEmpty()) {
      processBatch(batch);
    }
  }

  /**
   * 배치 단위로 Place를 처리하여 임베딩
   *
   * <p>각 Place를 Document로 변환하고 기존 임베딩을 삭제한 후 일괄 저장합니다.
   *
   * @param places 처리할 Place 리스트
   */
  private void processBatch(List<Place> places) {
    List<Document> documents = new ArrayList<>(places.size());

    for (Place place : places) {
      try {
        Document document = documentConverter.toDocument(place);
        documents.add(document);
        vectorStoreRepository.deleteByPlaceId(place.getId());
      } catch (Exception e) {
        log.error("Failed to process place: {}", place.getId(), e);
      }
    }

    if (!documents.isEmpty()) {
      vectorStoreRepository.saveAll(documents);
    }
  }

  /**
   * 단일 Place를 처리하여 임베딩
   *
   * <p>Place를 Document로 변환하고 기존 임베딩을 삭제한 후 저장합니다.
   *
   * @param place 처리할 Place
   */
  private void processPlace(Place place) {
    Document document = documentConverter.toDocument(place);
    vectorStoreRepository.deleteByPlaceId(place.getId());
    vectorStoreRepository.save(document);
  }
}
