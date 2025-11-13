package com.swygbro.airoad.backend.content.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.swygbro.airoad.backend.content.domain.dto.response.PlaceResponse;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlaceQueryService implements PlaceQueryUseCase {

  private final PlaceRepository placeRepository;

  @Override
  @Tool(
      description =
          """
              특정 지역과 테마 내에서 장소를 랜덤으로 추천합니다.
              사용자가 '어디 갈지 추천해줘'처럼 구체적인 장소 없이 막연하게 요청할 때, 혹은 새로운 장소를 발견하고 싶어할 때 사용하세요.
              """)
  public List<PlaceResponse> findRandomPlaces(
      @ToolParam(
              description =
                  """
                  장소 상세 주소 (반드시 전체 행정구역명 형식 사용)
                  - 특별시/광역시: "서울특별시", "인천광역시", "대전광역시" 등
                  - 도 단위: "경기도", "강원특별자치도", "충청남도" 등
                  - 시/군/구 포함: "경기도 가평군", "인천광역시 계양구", "서울특별시 강남구" 등
                  - 잘못된 예시: "인천 부평" (X), "서울" (X), "강남" (X)
                  - 올바른 예시: "인천광역시", "인천광역시 부평구", "서울특별시", "서울특별시 강남구"
                  """)
          String address,
      @ToolParam(
              description =
                  """
                  장소 테마 목록
                  - FAMOUS_SPOT: 유명 관광지
                  - HEALING: 힐링
                  - SNS_HOTSPOT: sns 핫플
                  - EXPERIENCE_ACTIVITY: 체험 액티비티
                  - CULTURE_ART: 문화/예술
                  - SHOPPING: 쇼핑
                  - RESTAURANT: 음식점
                  """,
              required = false)
          List<PlaceThemeType> themes,
      @ToolParam(description = "추천할 장소 개수") int limit) {

    log.debug("랜덤 장소 조회 시작 - address: {}, themes: {}, limit: {}", address, themes, limit);

    List<Long> allIds = placeRepository.findIdsByAddressStartingWithAndThemes(address, themes);

    log.debug("필터링된 장소 ID 개수: {}", allIds.size());

    if (allIds.isEmpty()) {
      log.warn("조건에 맞는 장소가 없습니다 - address: {}, themes: {}", address, themes);
      return Collections.emptyList();
    }

    List<Long> shuffledIds = new ArrayList<>(allIds);
    Collections.shuffle(shuffledIds);

    int actualLimit = Math.min(limit, shuffledIds.size());
    List<Long> selectedIds = shuffledIds.subList(0, actualLimit);

    log.debug("랜덤 샘플링 완료 - 선택된 ID 개수: {}", selectedIds.size());

    List<Place> places = placeRepository.findAllByIdsWithThemes(selectedIds);

    List<PlaceResponse> responses =
        places.stream().map(PlaceResponse::of).collect(Collectors.toList());

    log.debug(
        "랜덤 장소 조회 완료 - address: {}, themes: {}, 반환 개수: {}", address, themes, responses.size());

    return responses;
  }

  @Override
  @Tool(
      description =
          """
              이름, 주소, 테마 등 특정 조건으로 장소를 검색합니다.
              사용자가 'A라는 카페 정보 알려줘' 또는 'B동에 있는 맛집 찾아줘'처럼 명확한 검색 의도를 가지고 질문할 때 사용하세요.
              """)
  public List<PlaceResponse> findPlaceDetails(
      @ToolParam(description = "장소 이름 (부분 일치 가능)") String name,
      @ToolParam(description = "장소 주소 (부분 일치 가능)") String address,
      @ToolParam(description = "조회할 사이즈") int size) {
    PageRequest pageRequest = PageRequest.of(0, size);

    Page<Place> page = placeRepository.findByNameAndAddress(name, address, pageRequest);

    return page.getContent().stream().map(PlaceResponse::of).toList();
  }
}
