package com.swygbro.airoad.backend.tourdata.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.swygbro.airoad.backend.tourdata.domain.dto.TourApiDetailResponse;
import com.swygbro.airoad.backend.tourdata.domain.dto.TourApiListResponse;

import lombok.extern.slf4j.Slf4j;

/** TourAPI 4.0 호출 클라이언트 (RestClient 사용) */
@Slf4j
@Component
public class TourApiClient {

  private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorService2";
  private static final String AREA_BASED_LIST_PATH = "/areaBasedList2";
  private static final String DETAIL_COMMON_PATH = "/detailCommon2";
  private static final String DETAIL_INTRO_PATH = "/detailIntro2";
  private static final String MOBILE_OS = "ETC";
  private static final String MOBILE_APP = "AppTest";
  private static final String RESPONSE_TYPE = "json";

  private final RestClient restClient;

  @Value("${tour-api.service-key}")
  private String serviceKey;

  public TourApiClient(RestClient.Builder tourApiRestClientBuilder) {
    this.restClient = tourApiRestClientBuilder.baseUrl(BASE_URL).build();
  }

  /**
   * areaBasedList2 - 지역 기반 관광정보 조회
   *
   * @param areaCode 지역 코드 (1=서울, 31=경기 등)
   * @param pageNo 페이지 번호
   * @param numOfRows 한 페이지 결과 수
   * @return TourApiListResponse
   */
  public TourApiListResponse getAreaBasedList(Integer areaCode, Integer pageNo, Integer numOfRows) {
    log.info(
        "Calling TourAPI areaBasedList2: areaCode={}, pageNo={}, numOfRows={}",
        areaCode,
        pageNo,
        numOfRows);

    try {
      return restClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path(AREA_BASED_LIST_PATH)
                      .queryParam("ServiceKey", serviceKey)
                      .queryParam("areaCode", areaCode)
                      .queryParam("pageNo", pageNo)
                      .queryParam("numOfRows", numOfRows)
                      .queryParam("MobileOS", MOBILE_OS)
                      .queryParam("MobileApp", MOBILE_APP)
                      .queryParam("_type", RESPONSE_TYPE)
                      .queryParam("arrange", "A") // 제목순 정렬
                      .build())
          .retrieve()
          .body(TourApiListResponse.class);
    } catch (Exception e) {
      log.error("Failed to call TourAPI areaBasedList2: {}", e.getMessage(), e);
      throw new RuntimeException("TourAPI 호출 실패: " + e.getMessage(), e);
    }
  }

  /**
   * detailCommon2 - 공통정보 조회 (overview 포함)
   *
   * @param contentId 콘텐츠 ID
   * @return TourApiDetailResponse
   */
  public TourApiDetailResponse getDetailCommon(Long contentId) {
    log.info("Calling TourAPI detailCommon2: contentId={}", contentId);

    try {
      String responseBody =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path(DETAIL_COMMON_PATH)
                          .queryParam("ServiceKey", serviceKey)
                          .queryParam("contentId", contentId)
                          .queryParam("MobileOS", MOBILE_OS)
                          .queryParam("MobileApp", MOBILE_APP)
                          .queryParam("_type", RESPONSE_TYPE)
                          .build())
              .retrieve()
              .body(String.class);

      log.debug("TourAPI detailCommon2 raw response: {}", responseBody);

      // Jackson ObjectMapper를 사용하여 수동으로 역직렬화
      com.fasterxml.jackson.databind.ObjectMapper mapper =
          new com.fasterxml.jackson.databind.ObjectMapper();
      return mapper.readValue(responseBody, TourApiDetailResponse.class);
    } catch (Exception e) {
      log.error("Failed to call TourAPI detailCommon2: {}", e.getMessage(), e);
      throw new RuntimeException("TourAPI 호출 실패: " + e.getMessage(), e);
    }
  }

  /**
   * detailIntro2 - 소개정보 조회 (contentTypeId별 상세 정보)
   *
   * @param contentId 콘텐츠 ID
   * @param contentTypeId 콘텐츠 타입 ID (12=관광지, 14=문화시설, 15=축제공연행사, 등)
   * @return JSON 문자열 (원본 응답)
   */
  public String getDetailIntro(Long contentId, Integer contentTypeId) {
    log.info(
        "Calling TourAPI detailIntro2: contentId={}, contentTypeId={}", contentId, contentTypeId);

    try {
      return restClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path(DETAIL_INTRO_PATH)
                      .queryParam("ServiceKey", serviceKey)
                      .queryParam("contentId", contentId)
                      .queryParam("contentTypeId", contentTypeId)
                      .queryParam("MobileOS", MOBILE_OS)
                      .queryParam("MobileApp", MOBILE_APP)
                      .queryParam("_type", RESPONSE_TYPE)
                      .build())
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("Failed to call TourAPI detailIntro2: {}", e.getMessage(), e);
      throw new RuntimeException("TourAPI 호출 실패: " + e.getMessage(), e);
    }
  }
}
