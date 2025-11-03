package com.swygbro.airoad.backend.tourdata.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.tourdata.application.TourDataSyncUseCase;
import com.swygbro.airoad.backend.tourdata.domain.dto.TourDataSyncRequest;
import com.swygbro.airoad.backend.tourdata.domain.dto.TourDataSyncResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 관광 데이터 동기화 관리 API */
@Slf4j
@Tag(name = "Tour Data Sync", description = "관광 데이터 동기화 API")
@RestController
@RequestMapping("/api/admin/tourdata/sync")
@RequiredArgsConstructor
public class TourDataSyncController {

  private final TourDataSyncUseCase tourDataSyncUseCase;

  /**
   * Phase 1: TourAPI areaBasedList2로 기본 정보 동기화
   *
   * <p>지역 코드(areaCode)별로 관광지 기본 정보를 조회하여 DB에 저장합니다. 기본값으로 서울(1)과 경기(31) 지역만 처리하며, overview는 포함되지
   * 않습니다.
   *
   * @param request 동기화 요청 (areaCodes, numOfRows)
   * @return 저장된 Place 수
   */
  @Operation(
      summary = "기본 정보 동기화",
      description = "TourAPI areaBasedList2로 관광지 기본 정보를 동기화합니다 (overview 제외)")
  @PostMapping("/basic")
  public CommonResponse<TourDataSyncResponse> syncBasicInfo(
      @RequestBody(required = false) TourDataSyncRequest.BasicInfo request) {

    if (request == null) {
      request = TourDataSyncRequest.BasicInfo.builder().build();
    }

    log.info(
        "Starting basic info sync: areaCodes={}, numOfRows={}",
        request.getAreaCodes(),
        request.getNumOfRows());

    int totalSaved =
        tourDataSyncUseCase.syncBasicInfo(request.getAreaCodes(), request.getNumOfRows());

    TourDataSyncResponse response =
        TourDataSyncResponse.builder()
            .totalProcessed(totalSaved)
            .message(String.format("기본 정보 동기화 완료: %d개 저장", totalSaved))
            .build();

    return CommonResponse.success(HttpStatus.OK, response);
  }

  /**
   * Phase 2: TourAPI detailCommon2로 overview 업데이트
   *
   * <p>description이 null인 Place에 대해 detailCommon2 API를 호출하여 overview를 업데이트합니다. API 트래픽 제한(일일
   * 1,000건)을 고려하여 배치 크기와 지연 시간을 설정할 수 있습니다.
   *
   * @param request 동기화 요청 (batchSize, delayMillis)
   * @return 업데이트된 Place 수
   */
  @Operation(summary = "상세 정보 동기화", description = "TourAPI detailCommon2로 overview를 업데이트합니다")
  @PostMapping("/detail")
  public CommonResponse<TourDataSyncResponse> syncDetailInfo(
      @RequestBody(required = false) TourDataSyncRequest.DetailInfo request) {

    if (request == null) {
      request = TourDataSyncRequest.DetailInfo.builder().build();
    }

    log.info(
        "Starting detail info sync: batchSize={}, delayMillis={}",
        request.getBatchSize(),
        request.getDelayMillis());

    int totalUpdated =
        tourDataSyncUseCase.syncDetailInfo(request.getBatchSize(), request.getDelayMillis());

    TourDataSyncResponse response =
        TourDataSyncResponse.builder()
            .totalProcessed(totalUpdated)
            .message(String.format("상세 정보 동기화 완료: %d개 업데이트", totalUpdated))
            .build();

    return CommonResponse.success(HttpStatus.OK, response);
  }
}
