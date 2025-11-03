package com.swygbro.airoad.backend.tourdata.application;

import java.util.List;

/** TourAPI 데이터 동기화 UseCase */
public interface TourDataSyncUseCase {

  /**
   * Phase 1: areaBasedList2로 기본 정보 동기화
   *
   * @param areaCodes 지역 코드 목록 (1=서울, 31=경기 등)
   * @param numOfRows 페이지당 결과 수
   * @return 저장된 Place 수
   */
  int syncBasicInfo(List<Integer> areaCodes, Integer numOfRows);

  /**
   * Phase 2: detailCommon2로 overview 업데이트
   *
   * @param batchSize 배치 크기
   * @param delayMillis API 호출 간 지연 시간 (ms)
   * @return 업데이트된 Place 수
   */
  int syncDetailInfo(Integer batchSize, Long delayMillis);

  /**
   * 테스트용: 1페이지만 동기화 (전체 페이징 없이)
   *
   * @param areaCodes 지역 코드 목록
   * @param numOfRows 가져올 개수
   * @return 저장된 Place 수
   */
  int syncBasicInfoSinglePage(List<Integer> areaCodes, Integer numOfRows);
}
