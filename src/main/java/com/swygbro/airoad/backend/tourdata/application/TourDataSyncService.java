package com.swygbro.airoad.backend.tourdata.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.swygbro.airoad.backend.common.domain.embeddable.Location;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.domain.entity.PlaceTheme;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.tourdata.domain.CategoryType;
import com.swygbro.airoad.backend.tourdata.domain.dto.TourApiDetailResponse;
import com.swygbro.airoad.backend.tourdata.domain.dto.TourApiListResponse;
import com.swygbro.airoad.backend.tourdata.infrastructure.client.TourApiClient;
import com.swygbro.airoad.backend.trip.infrastructure.repository.PlaceThemeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** TourAPI 데이터 동기화 서비스 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TourDataSyncService implements TourDataSyncUseCase {

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
  private static final int DEFAULT_NUM_OF_ROWS = 100;
  private static final int DEFAULT_BATCH_SIZE = 50;
  private static final long DEFAULT_DELAY_MILLIS = 1000L;

  private final TourApiClient tourApiClient;
  private final PlaceRepository placeRepository;
  private final PlaceThemeRepository placeThemeRepository;

  @Override
  public int syncBasicInfo(List<Integer> areaCodes, Integer numOfRows) {
    int totalSaved = 0;
    int rows = numOfRows != null ? numOfRows : DEFAULT_NUM_OF_ROWS;

    for (Integer areaCode : areaCodes) {
      log.info("Starting basic info sync for areaCode: {}", areaCode);
      totalSaved += syncAreaCode(areaCode, rows);
    }

    log.info("Basic info sync completed. Total saved: {}", totalSaved);
    return totalSaved;
  }

  /**
   * 특정 지역 코드에 대한 전체 데이터 동기화
   *
   * @param areaCode 지역 코드
   * @param numOfRows 페이지당 결과 수
   * @return 저장된 Place 수
   */
  private int syncAreaCode(Integer areaCode, Integer numOfRows) {
    int pageNo = 1;
    int totalSaved = 0;
    boolean hasMore = true;

    while (hasMore) {
      TourApiListResponse response = tourApiClient.getAreaBasedList(areaCode, pageNo, numOfRows);

      if (response == null
          || response.getResponse() == null
          || response.getResponse().getBody() == null
          || response.getResponse().getBody().getItems() == null
          || response.getResponse().getBody().getItems().getItem() == null) {
        log.warn("No more data for areaCode: {}, pageNo: {}", areaCode, pageNo);
        break;
      }

      List<TourApiListResponse.Item> items = response.getResponse().getBody().getItems().getItem();
      if (items.isEmpty()) {
        break;
      }

      // 페이지 단위로 트랜잭션 분할하여 저장
      int savedCount = syncPageWithTransaction(items, areaCode, pageNo);
      totalSaved += savedCount;

      // 다음 페이지 확인
      int totalCount = response.getResponse().getBody().getTotalCount();
      int currentEnd = pageNo * numOfRows;
      hasMore = currentEnd < totalCount;
      pageNo++;

      // API 호출 제한 고려하여 짧은 딜레이
      if (hasMore) {
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.warn("Sleep interrupted");
        }
      }
    }

    return totalSaved;
  }

  /**
   * 페이지 단위 데이터를 별도 트랜잭션으로 저장
   *
   * @param items API 응답 아이템 목록
   * @param areaCode 지역 코드
   * @param pageNo 페이지 번호
   * @return 저장된 개수
   */
  @Transactional
  private int syncPageWithTransaction(
      List<TourApiListResponse.Item> items, Integer areaCode, Integer pageNo) {

    List<Place> places = new ArrayList<>();
    for (TourApiListResponse.Item item : items) {
      try {
        Optional<Place> existingPlace = placeRepository.findByApiPlaceId(item.getContentid());
        if (existingPlace.isEmpty()) {
          Place place = convertToPlace(item);
          places.add(place);
        } else {
          log.debug("Place already exists: apiPlaceId={}", item.getContentid());
        }
      } catch (Exception e) {
        log.error(
            "Failed to convert item to Place: apiPlaceId={}, error={}",
            item.getContentid(),
            e.getMessage());
      }
    }

    if (places.isEmpty()) {
      return 0;
    }

    List<Place> savedPlaces = placeRepository.saveAll(places);

    // PlaceTheme 엔티티 생성 및 저장
    List<PlaceTheme> placeThemes = new ArrayList<>();
    for (Place savedPlace : savedPlaces) {
      if (!savedPlace.getThemes().isEmpty()) {
        for (PlaceThemeType themeType : savedPlace.getThemes()) {
          PlaceTheme placeTheme =
              PlaceTheme.builder().place(savedPlace).placeThemeType(themeType).build();
          placeThemes.add(placeTheme);
        }
      }
    }

    if (!placeThemes.isEmpty()) {
      placeThemeRepository.saveAll(placeThemes);
      log.info(
          "Saved {} place themes for areaCode: {}, pageNo: {}",
          placeThemes.size(),
          areaCode,
          pageNo);
    }

    log.info("Saved {} places for areaCode: {}, pageNo: {}", savedPlaces.size(), areaCode, pageNo);
    return savedPlaces.size();
  }

  /**
   * TourAPI Item을 Place 엔티티로 변환
   *
   * @param item TourAPI Item
   * @return Place 엔티티
   */
  private Place convertToPlace(TourApiListResponse.Item item) {
    // 주소 생성
    String address = item.getAddr1();
    if (StringUtils.hasText(item.getAddr2())) {
      address = address + " " + item.getAddr2();
    }

    // Point 생성 (경도, 위도)
    Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(item.getMapx(), item.getMapy()));

    // Location 생성
    Location location =
        Location.builder().name(item.getTitle()).address(address).point(point).build();

    // 이미지 URL (firstimage 우선, 없으면 firstimage2)
    String imageUrl =
        StringUtils.hasText(item.getFirstimage()) ? item.getFirstimage() : item.getFirstimage2();

    // Cat2 코드로 PlaceTheme 매핑
    Place.PlaceBuilder builder =
        Place.builder()
            .location(location)
            .apiPlaceId(item.getContentid())
            .imageUrl(imageUrl)
            .isMustVisit(false);

    // Cat2 코드가 있으면 테마 추가
    log.debug(
        "Converting place: apiPlaceId={}, title={}, cat2={}",
        item.getContentid(),
        item.getTitle(),
        item.getCat2());

    if (StringUtils.hasText(item.getCat2())) {
      CategoryType.Cat2 cat2 = CategoryType.Cat2.fromCode(item.getCat2());
      log.debug("Cat2 mapping result: {} -> {}", item.getCat2(), cat2);
      if (cat2 != null) {
        java.util.Set<PlaceThemeType> themes = cat2.getThemes();
        log.debug("Themes for cat2 {}: {}", item.getCat2(), themes);
        themes.forEach(builder::theme);
      } else {
        log.warn("No Cat2 mapping found for code: {}", item.getCat2());
      }
    } else {
      log.debug("No cat2 code for apiPlaceId: {}", item.getContentid());
    }

    return builder.build();
  }

  @Override
  @Transactional
  public int syncDetailInfo(Integer batchSize, Long delayMillis) {
    int size = batchSize != null ? batchSize : DEFAULT_BATCH_SIZE;
    long delay = delayMillis != null ? delayMillis : DEFAULT_DELAY_MILLIS;

    // description이 null인 Place 조회
    List<Place> places = placeRepository.findByDescriptionIsNull();
    log.info("Found {} places without description", places.size());

    if (places.isEmpty()) {
      log.warn("No places found without description. Skipping detail sync.");
      return 0;
    }

    int totalUpdated = 0;
    int count = 0;

    for (Place place : places) {
      try {
        log.info(
            "Fetching detail for place: id={}, apiPlaceId={}, name={}",
            place.getId(),
            place.getApiPlaceId(),
            place.getLocation() != null ? place.getLocation().getName() : "N/A");

        TourApiDetailResponse response = tourApiClient.getDetailCommon(place.getApiPlaceId());

        if (response == null) {
          log.warn("Response is null for apiPlaceId: {}", place.getApiPlaceId());
          continue;
        }

        if (response.getResponse() == null) {
          log.warn("Response.response is null for apiPlaceId: {}", place.getApiPlaceId());
          continue;
        }

        if (response.getResponse().getBody() == null) {
          log.warn("Response.body is null for apiPlaceId: {}", place.getApiPlaceId());
          continue;
        }

        if (response.getResponse().getBody().getItems() == null) {
          log.warn("Response.items is null for apiPlaceId: {}", place.getApiPlaceId());
          continue;
        }

        if (response.getResponse().getBody().getItems().getItem() == null
            || response.getResponse().getBody().getItems().getItem().isEmpty()) {
          log.warn("Response.item is empty for apiPlaceId: {}", place.getApiPlaceId());
          continue;
        }

        TourApiDetailResponse.Item item =
            response.getResponse().getBody().getItems().getItem().get(0);

        if (StringUtils.hasText(item.getOverview())) {
          place.updateDescription(item.getOverview());
          placeRepository.save(place);
          totalUpdated++;
          log.info(
              "Updated description for apiPlaceId: {}, overview length: {}",
              place.getApiPlaceId(),
              item.getOverview().length());
        } else {
          log.warn("Overview is empty for apiPlaceId: {}", place.getApiPlaceId());
        }

        count++;

        // 배치 크기마다 딜레이 (API 트래픽 제한 고려)
        if (count % size == 0) {
          log.info("Processed {}/{} places", count, places.size());
          Thread.sleep(delay);
        }

      } catch (Exception e) {
        log.error(
            "Failed to sync detail for apiPlaceId: {}, error: {}",
            place.getApiPlaceId(),
            e.getMessage(),
            e);
      }
    }

    log.info("Detail info sync completed. Total updated: {}/{}", totalUpdated, places.size());
    return totalUpdated;
  }

  @Override
  @Transactional
  public int syncBasicInfoSinglePage(List<Integer> areaCodes, Integer numOfRows) {
    int totalSaved = 0;
    int rows = numOfRows != null ? numOfRows : DEFAULT_NUM_OF_ROWS;

    for (Integer areaCode : areaCodes) {
      log.info("Starting single page sync for areaCode: {}, numOfRows: {}", areaCode, rows);

      TourApiListResponse response = tourApiClient.getAreaBasedList(areaCode, 1, rows);

      if (response == null
          || response.getResponse() == null
          || response.getResponse().getBody() == null
          || response.getResponse().getBody().getItems() == null
          || response.getResponse().getBody().getItems().getItem() == null) {
        log.warn("No data for areaCode: {}", areaCode);
        continue;
      }

      List<TourApiListResponse.Item> items = response.getResponse().getBody().getItems().getItem();
      if (items.isEmpty()) {
        log.warn("Empty items for areaCode: {}", areaCode);
        continue;
      }

      List<Place> places = new ArrayList<>();
      for (TourApiListResponse.Item item : items) {
        try {
          Optional<Place> existingPlace = placeRepository.findByApiPlaceId(item.getContentid());
          if (existingPlace.isEmpty()) {
            Place place = convertToPlace(item);
            places.add(place);
          } else {
            log.info("Place already exists: apiPlaceId={}", item.getContentid());
          }
        } catch (Exception e) {
          log.error(
              "Failed to convert item: apiPlaceId={}, error={}",
              item.getContentid(),
              e.getMessage(),
              e);
        }
      }

      if (!places.isEmpty()) {
        List<Place> savedPlaces = placeRepository.saveAll(places);

        // PlaceTheme 엔티티 생성 및 저장
        List<PlaceTheme> placeThemes = new ArrayList<>();
        for (Place savedPlace : savedPlaces) {
          if (!savedPlace.getThemes().isEmpty()) {
            for (PlaceThemeType themeType : savedPlace.getThemes()) {
              PlaceTheme placeTheme =
                  PlaceTheme.builder().place(savedPlace).placeThemeType(themeType).build();
              placeThemes.add(placeTheme);
            }
          }
        }

        if (!placeThemes.isEmpty()) {
          placeThemeRepository.saveAll(placeThemes);
          log.info("Saved {} place themes for areaCode: {}", placeThemes.size(), areaCode);
        }

        totalSaved += savedPlaces.size();
        log.info("Saved {} places for areaCode: {}", savedPlaces.size(), areaCode);
      } else {
        log.info(
            "No new places to save for areaCode: {} (all already exist or conversion failed)",
            areaCode);
      }
    }

    log.info("Single page sync completed. Total saved: {}", totalSaved);
    return totalSaved;
  }
}
