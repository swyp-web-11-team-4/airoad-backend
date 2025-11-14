package com.swygbro.airoad.backend.content.application;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Place 임베딩 스케줄러
 *
 * <p>정기적으로 Place 데이터를 임베딩하여 벡터 스토어를 최신 상태로 유지합니다.
 *
 * <ul>
 *   <li>증분 업데이트: 매일 한국 시간 02:00 - 최근 24시간 수정본만 처리
 *   <li>전체 재임베딩: 주 1회 (일요일 한국 시간 03:00) - 전체 데이터 정합성 보장
 * </ul>
 *
 * <p>PlaceEmbeddingUseCase Bean이 존재할 때만 활성화됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(PlaceEmbeddingUseCase.class)
public class PlaceEmbeddingScheduler {

  private final PlaceEmbeddingUseCase placeEmbeddingUseCase;

  /** 매일 한국 시간 02:00 (UTC 17:00 전날)에 최근 24시간 수정된 Place 삭제 후 재생성 */
  //  @Scheduled(cron = "0 0 17 * * ?", zone = "UTC")
  public void embedModifiedPlacesDaily() {
    log.info("Starting scheduled incremental Place embedding job");
    try {
      LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minusHours(24);
      placeEmbeddingUseCase.embedModifiedPlaces(since);
      log.info("Scheduled incremental Place embedding completed successfully");
    } catch (Exception e) {
      log.error("Scheduled incremental Place embedding failed", e);
    }
  }

  /** 매주 일요일 한국 시간 03:00 (UTC 토요일 18:00)에 전체 Place 데이터를 재임베딩 */
  //  @Scheduled(cron = "0 0 18 ? * SAT", zone = "UTC")
  public void embedAllPlacesWeekly() {
    log.info("Starting scheduled full Place embedding job");
    try {
      placeEmbeddingUseCase.embedAllPlaces();
      log.info("Scheduled full Place embedding completed successfully");
    } catch (Exception e) {
      log.error("Scheduled full Place embedding failed", e);
    }
  }
}
