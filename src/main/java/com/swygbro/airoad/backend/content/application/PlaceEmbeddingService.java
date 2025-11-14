package com.swygbro.airoad.backend.content.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.util.concurrent.RateLimiter;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
import com.swygbro.airoad.backend.content.domain.event.PlaceSummaryRequestedEvent;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceEmbeddingService implements PlaceEmbeddingUseCase {

  /**
   * 배치 사이즈는 반드시 1로 고정해야 합니다. 네이버 CLOVA X의 경우 임베딩에 필요한 input 필드 지원을 String 형식만 지원합니다. 이번 MVP에서는 구현이
   * 우선이므로 여행지 장소 임베딩을 단건 처리로 API 요청을 보내도록 하고 추후 청킹 전략으로 전환이 필요합니다.
   */
  private static final int BATCH_SIZE = 1;

  /**
   * API Rate Limiter (초당 최대 1개로 제한)
   *
   * <p>Naver ClovaX API의 Rate Limit (분당 60회)를 초과하지 않도록 초당 최대 1개의 요청만 허용합니다.
   */
  private final RateLimiter rateLimiter = RateLimiter.create(55.0 / 60.0);

  private final PlaceRepository placeRepository;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * 모든 Place 데이터를 임베딩하여 벡터 스토어에 저장
   *
   * <p>Stream + Batch 방식으로 메모리 효율적으로 처리하며, 이벤트를 발행하여 비동기 처리합니다.
   */
  @Override
  @Transactional(readOnly = true)
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
  @Transactional(readOnly = true)
  public void embedModifiedPlaces(LocalDateTime since) {
    log.info("Starting to embed places modified after: {}", since);
    try (Stream<Place> placeStream = placeRepository.streamByUpdatedAtAfter(since)) {
      processPlaceStream(placeStream);
    }
  }

  /**
   * 특정 Place를 임베딩하여 벡터 스토어에 저장
   *
   * <p>이벤트를 발행하여 비동기 처리합니다.
   *
   * @param placeId 임베딩할 Place의 ID
   * @throws IllegalArgumentException Place를 찾을 수 없는 경우
   */
  @Override
  @Transactional(readOnly = true)
  public void embedPlace(Long placeId) {
    log.info("Embedding place with ID: {}", placeId);

    Place place =
        placeRepository
            .findByIdWithThemes(placeId)
            .orElseThrow(
                () -> {
                  log.error("Place not found with ID: {}", placeId);
                  return new IllegalArgumentException("Place not found with ID: " + placeId);
                });

    publishPlaceSummaryEvent(place);

    log.info(
        "PlaceSummaryRequestedEvent published - placeId: {}, name: {}",
        placeId,
        place.getLocation().getName());
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
   * 배치 단위로 Place를 처리하여 이벤트 발행
   *
   * <p>isMustVisit = true인 경우에만 이벤트를 발행합니다.
   *
   * @param places 처리할 Place 리스트
   */
  private void processBatch(List<Place> places) {
    for (Place place : places) {
      try {
        if (!place.getIsMustVisit()) {
          continue;
        }

        publishPlaceSummaryEvent(place);
      } catch (Exception e) {
        log.error("Failed to publish event for place: {}", place.getId(), e);
      }
    }
  }

  /**
   * PlaceSummaryRequestedEvent 발행
   *
   * <p>RateLimiter를 통해 초당 10개로 이벤트 발행 속도를 제한하여 API Rate Limit 초과를 방지합니다.
   *
   * @param place 처리할 Place
   */
  private void publishPlaceSummaryEvent(Place place) {
    rateLimiter.acquire();

    List<String> themes = place.getThemes().stream().map(PlaceThemeType::getDescription).toList();

    PlaceSummaryRequestedEvent event =
        PlaceSummaryRequestedEvent.builder()
            .placeId(place.getId())
            .name(place.getLocation().getName())
            .address(place.getLocation().getAddress())
            .description(place.getDescription())
            .operatingHours(place.getOperatingHours())
            .holidayInfo(place.getHolidayInfo())
            .themes(themes)
            .build();

    eventPublisher.publishEvent(event);
    log.debug("PlaceSummaryRequestedEvent published - placeId: {}", place.getId());
  }
}
