package com.swygbro.airoad.backend.tourdata.domain.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** TourAPI 데이터 동기화 요청 DTO */
@Getter
@NoArgsConstructor
public class TourDataSyncRequest {

  /** Phase 1: 기본 정보 동기화 요청 */
  @Getter
  @NoArgsConstructor
  public static class BasicInfo {
    /** 지역 코드 목록 (기본값: [1, 31] = 서울, 경기) */
    private List<Integer> areaCodes = List.of(1, 31);

    /** 페이지당 결과 수 (기본값: 100) */
    private Integer numOfRows = 100;

    @Builder
    public BasicInfo(List<Integer> areaCodes, Integer numOfRows) {
      if (areaCodes != null && !areaCodes.isEmpty()) {
        this.areaCodes = areaCodes;
      }
      if (numOfRows != null) {
        this.numOfRows = numOfRows;
      }
    }
  }

  /** Phase 2: 상세 정보 동기화 요청 */
  @Getter
  @NoArgsConstructor
  public static class DetailInfo {
    /** 배치 크기 (기본값: 50) */
    private Integer batchSize = 50;

    /** API 호출 간 지연 시간(ms) (기본값: 1000ms = 1초) */
    private Long delayMillis = 1000L;

    @Builder
    public DetailInfo(Integer batchSize, Long delayMillis) {
      if (batchSize != null) {
        this.batchSize = batchSize;
      }
      if (delayMillis != null) {
        this.delayMillis = delayMillis;
      }
    }
  }
}
