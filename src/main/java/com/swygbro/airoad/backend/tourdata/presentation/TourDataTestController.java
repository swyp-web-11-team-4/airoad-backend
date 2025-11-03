package com.swygbro.airoad.backend.tourdata.presentation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.tourdata.application.TourDataSyncUseCase;
import com.swygbro.airoad.backend.tourdata.domain.dto.TourApiDetailResponse;
import com.swygbro.airoad.backend.tourdata.domain.dto.TourApiListResponse;
import com.swygbro.airoad.backend.tourdata.domain.dto.TourDataSyncResponse;
import com.swygbro.airoad.backend.tourdata.infrastructure.client.TourApiClient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 관광 데이터 테스트용 API (소량 데이터 확인용) */
@Slf4j
@Tag(name = "Tour Data Test", description = "관광 데이터 테스트 API (소량 데이터)")
@RestController
@RequestMapping("/api/admin/tourdata/test")
@RequiredArgsConstructor
public class TourDataTestController {

  private final TourDataSyncUseCase tourDataSyncUseCase;
  private final TourApiClient tourApiClient;
  private final PlaceRepository placeRepository;

  /**
   * 테스트: 서울 지역에서 10개만 가져오기 (1페이지만)
   *
   * @param numOfRows 가져올 개수 (기본 10개)
   * @return 저장된 Place 수
   */
  @Operation(summary = "테스트 동기화 (소량)", description = "서울 지역에서 1페이지(소량)의 데이터만 가져와서 테스트합니다")
  @GetMapping("/sync-small")
  public CommonResponse<TourDataSyncResponse> syncSmall(
      @RequestParam(defaultValue = "10") Integer numOfRows) {

    log.info("Starting small test sync (single page): numOfRows={}", numOfRows);

    // 서울(1) 지역에서 1페이지만 가져오기
    int totalSaved = tourDataSyncUseCase.syncBasicInfoSinglePage(List.of(1), numOfRows);

    TourDataSyncResponse response =
        TourDataSyncResponse.builder()
            .totalProcessed(totalSaved)
            .message(String.format("테스트 동기화 완료: %d개 저장 (서울 1페이지)", totalSaved))
            .build();

    return CommonResponse.success(HttpStatus.OK, response);
  }

  /**
   * 테스트: description이 없는 Place에 대해 detail 정보 가져오기
   *
   * @param batchSize 배치 크기 (기본 10)
   * @param delayMillis API 호출 간 지연 시간 (기본 1000ms)
   * @return 업데이트된 Place 수
   */
  @Operation(
      summary = "Detail 정보 동기화 (테스트)",
      description = "description이 없는 Place에 대해 detailCommon2 API를 호출하여 overview를 가져옵니다")
  @GetMapping("/sync-detail")
  public CommonResponse<TourDataSyncResponse> syncDetail(
      @RequestParam(defaultValue = "10") Integer batchSize,
      @RequestParam(defaultValue = "1000") Long delayMillis) {

    log.info("Starting detail info sync: batchSize={}, delayMillis={}", batchSize, delayMillis);

    int totalUpdated = tourDataSyncUseCase.syncDetailInfo(batchSize, delayMillis);

    TourDataSyncResponse response =
        TourDataSyncResponse.builder()
            .totalProcessed(totalUpdated)
            .message(String.format("Detail 정보 동기화 완료: %d개 업데이트", totalUpdated))
            .build();

    return CommonResponse.success(HttpStatus.OK, response);
  }

  /**
   * 테스트: 특정 contentId에 대한 detail 정보 원본 응답 확인
   *
   * @param contentId TourAPI contentId
   * @return TourAPI 원본 응답
   */
  @Operation(
      summary = "Detail API 원본 응답 확인",
      description = "특정 contentId에 대한 detailCommon2 API 원본 응답을 확인합니다")
  @GetMapping("/detail-raw/{contentId}")
  public CommonResponse<TourApiDetailResponse> getDetailRaw(@PathVariable Long contentId) {

    log.info("Fetching raw detail response for contentId: {}", contentId);

    TourApiDetailResponse response = tourApiClient.getDetailCommon(contentId);

    log.info("Response received: {}", response != null ? "NOT NULL" : "NULL");
    if (response != null && response.getResponse() != null) {
      log.info("Response.response: NOT NULL");
      if (response.getResponse().getBody() != null) {
        log.info("Response.body: NOT NULL");
        if (response.getResponse().getBody().getItems() != null) {
          log.info("Response.items: NOT NULL");
          if (response.getResponse().getBody().getItems().getItem() != null) {
            log.info(
                "Response.item size: {}",
                response.getResponse().getBody().getItems().getItem().size());
            if (!response.getResponse().getBody().getItems().getItem().isEmpty()) {
              TourApiDetailResponse.Item item =
                  response.getResponse().getBody().getItems().getItem().get(0);
              log.info(
                  "Item overview length: {}",
                  item.getOverview() != null ? item.getOverview().length() : 0);
              log.info("Item overview: {}", item.getOverview());
            }
          }
        }
      }
    }

    return CommonResponse.success(HttpStatus.OK, response);
  }

  /**
   * 테스트: 서울 지역 10개 데이터를 JSON 파일로 저장
   *
   * @param numOfRows 가져올 개수 (기본 10개)
   * @return 파일 저장 결과
   */
  @Operation(summary = "JSON 파일로 저장", description = "서울 지역 데이터를 가져와서 바탕화면의 json.txt 파일로 저장합니다")
  @GetMapping("/save-json")
  public CommonResponse<String> saveToJsonFile(
      @RequestParam(defaultValue = "10") Integer numOfRows) {

    log.info("Fetching data to save as JSON file: numOfRows={}", numOfRows);

    try {
      // 서울(1) 지역 데이터 가져오기
      TourApiListResponse response = tourApiClient.getAreaBasedList(1, 1, numOfRows);

      if (response == null
          || response.getResponse() == null
          || response.getResponse().getBody() == null
          || response.getResponse().getBody().getItems() == null) {
        return CommonResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "API 응답이 비어있습니다");
      }

      // JSON으로 변환 (pretty print)
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
      String jsonString = objectMapper.writeValueAsString(response);

      // 바탕화면 경로
      String desktopPath = System.getProperty("user.home") + "\\Desktop\\json.txt";

      // 파일로 저장
      try (FileWriter fileWriter = new FileWriter(desktopPath)) {
        fileWriter.write(jsonString);
      }

      log.info("JSON file saved successfully to: {}", desktopPath);
      return CommonResponse.success(
          HttpStatus.OK, "파일 저장 완료: " + desktopPath + " (총 " + numOfRows + "개 데이터)");

    } catch (IOException e) {
      log.error("Failed to save JSON file: {}", e.getMessage(), e);
      return CommonResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 실패: " + e.getMessage());
    }
  }

  /**
   * 테스트: DB의 Place로 detailIntro2 API 호출하여 JSON 파일로 저장
   *
   * @param limit 조회할 Place 개수 (기본 10개)
   * @param contentTypeId 콘텐츠 타입 ID (기본 12=관광지)
   * @return 파일 저장 결과
   */
  @Operation(
      summary = "DetailIntro JSON 파일로 저장",
      description = "DB에 저장된 Place의 contentId로 detailIntro2 API를 호출하여 바탕화면의 json.txt 파일로 저장합니다")
  @GetMapping("/save-detail-intro-json")
  public CommonResponse<String> saveDetailIntroToJsonFile(
      @RequestParam(defaultValue = "10") Integer limit,
      @RequestParam(defaultValue = "12") Integer contentTypeId) {

    log.info(
        "Fetching detailIntro data to save as JSON file: limit={}, contentTypeId={}",
        limit,
        contentTypeId);

    try {
      // DB에서 Place 조회 (limit 개수만큼)
      List<Place> places = placeRepository.findAll().stream().limit(limit).toList();

      if (places.isEmpty()) {
        return CommonResponse.error(HttpStatus.NOT_FOUND, "DB에 Place 데이터가 없습니다");
      }

      // 첫 번째 Place의 contentId로 API 호출
      Place firstPlace = places.get(0);
      Long contentId = firstPlace.getApiPlaceId();

      log.info(
          "Calling detailIntro2 API: contentId={}, contentTypeId={}", contentId, contentTypeId);

      // detailIntro2 API 호출 (JSON 문자열로 반환)
      String jsonResponse = tourApiClient.getDetailIntro(contentId, contentTypeId);

      // Pretty print를 위해 ObjectMapper로 포맷팅
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
      Object jsonObject = objectMapper.readValue(jsonResponse, Object.class);
      String prettyJson = objectMapper.writeValueAsString(jsonObject);

      // 바탕화면 경로
      String desktopPath = System.getProperty("user.home") + "\\Desktop\\json.txt";

      // 파일로 저장
      try (FileWriter fileWriter = new FileWriter(desktopPath)) {
        fileWriter.write(prettyJson);
      }

      log.info("DetailIntro JSON file saved successfully to: {}", desktopPath);
      return CommonResponse.success(
          HttpStatus.OK,
          String.format(
              "파일 저장 완료: %s (contentId=%d, contentTypeId=%d)",
              desktopPath, contentId, contentTypeId));

    } catch (IOException e) {
      log.error("Failed to save DetailIntro JSON file: {}", e.getMessage(), e);
      return CommonResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 실패: " + e.getMessage());
    } catch (Exception e) {
      log.error("Failed to fetch DetailIntro data: {}", e.getMessage(), e);
      return CommonResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "API 호출 실패: " + e.getMessage());
    }
  }
}
