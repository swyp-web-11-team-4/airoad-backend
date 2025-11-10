package com.swygbro.airoad.backend.content.application;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.content.domain.dto.response.PlaceResponse;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.fixture.content.PlaceFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PlaceQueryServiceTest {

  @Mock private PlaceRepository placeRepository;

  @InjectMocks private PlaceQueryService placeQueryService;

  @Nested
  @DisplayName("주소와 테마로 랜덤 장소 조회 시")
  class FindRandomPlaces {

    @Test
    @DisplayName("주소와 테마에 맞는 장소들을 조회하면 랜덤하게 반환한다")
    void givenAddressAndThemes_whenQuery_thenReturnRandomPlaces() {
      // given: 조회 조건
      String address = "서울특별시";
      List<PlaceThemeType> themes = List.of(PlaceThemeType.HEALING, PlaceThemeType.CULTURE_ART);
      int limit = 5;

      // given: 조건에 맞는 장소 ID들
      List<Long> placeIds = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
      given(placeRepository.findIdsByAddressStartingWithAndThemes(address, themes))
          .willReturn(placeIds);

      // given: ID로 조회된 장소들
      List<Place> places =
          List.of(
              PlaceFixture.withId(1L, PlaceFixture.create()),
              PlaceFixture.withId(2L, PlaceFixture.create()),
              PlaceFixture.withId(3L, PlaceFixture.create()),
              PlaceFixture.withId(4L, PlaceFixture.create()),
              PlaceFixture.withId(5L, PlaceFixture.create()));
      given(placeRepository.findAllByIdsWithThemes(org.mockito.ArgumentMatchers.anyList()))
          .willReturn(places);

      // when: 랜덤 장소 조회
      List<PlaceResponse> result = placeQueryService.findRandomPlaces(address, themes, limit);

      // then: Repository 호출됨
      then(placeRepository).should(times(1)).findIdsByAddressStartingWithAndThemes(address, themes);
      then(placeRepository)
          .should(times(1))
          .findAllByIdsWithThemes(org.mockito.ArgumentMatchers.anyList());

      // then: 요청한 개수만큼 반환됨
      assertThat(result).hasSize(5);
    }

    @Test
    @DisplayName("조건에 맞는 장소가 없으면 빈 리스트를 반환한다")
    void givenNoMatchingPlaces_whenQuery_thenReturnEmptyList() {
      // given: 조건에 맞는 장소가 없음
      String address = "존재하지않는주소";
      List<PlaceThemeType> themes = List.of(PlaceThemeType.HEALING);
      int limit = 5;

      given(placeRepository.findIdsByAddressStartingWithAndThemes(address, themes))
          .willReturn(Collections.emptyList());

      // when: 랜덤 장소 조회
      List<PlaceResponse> result = placeQueryService.findRandomPlaces(address, themes, limit);

      // then: 빈 리스트 반환됨
      assertThat(result).isEmpty();

      // then: findAllByIdsWithThemes는 호출되지 않음
      then(placeRepository)
          .should(times(0))
          .findAllByIdsWithThemes(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("요청한 개수보다 적은 장소만 있으면 있는 만큼만 반환한다")
    void givenLessThanLimit_whenQuery_thenReturnAvailablePlaces() {
      // given: 3개만 존재
      String address = "서울특별시";
      List<PlaceThemeType> themes = List.of(PlaceThemeType.HEALING);
      int limit = 10;

      List<Long> placeIds = List.of(1L, 2L, 3L);
      given(placeRepository.findIdsByAddressStartingWithAndThemes(address, themes))
          .willReturn(placeIds);

      List<Place> places =
          List.of(
              PlaceFixture.withId(1L, PlaceFixture.create()),
              PlaceFixture.withId(2L, PlaceFixture.create()),
              PlaceFixture.withId(3L, PlaceFixture.create()));
      given(placeRepository.findAllByIdsWithThemes(org.mockito.ArgumentMatchers.anyList()))
          .willReturn(places);

      // when: 10개 요청
      List<PlaceResponse> result = placeQueryService.findRandomPlaces(address, themes, limit);

      // then: 3개만 반환됨
      assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("테마 없이 조회하면 주소만으로 필터링한다")
    void givenOnlyAddress_whenQuery_thenFilterByAddressOnly() {
      // given: 테마 없이 주소만
      String address = "경기도";
      List<PlaceThemeType> themes = null;
      int limit = 3;

      List<Long> placeIds = List.of(1L, 2L, 3L);
      given(placeRepository.findIdsByAddressStartingWithAndThemes(address, themes))
          .willReturn(placeIds);

      List<Place> places =
          List.of(
              PlaceFixture.withId(1L, PlaceFixture.create()),
              PlaceFixture.withId(2L, PlaceFixture.create()),
              PlaceFixture.withId(3L, PlaceFixture.create()));
      given(placeRepository.findAllByIdsWithThemes(org.mockito.ArgumentMatchers.anyList()))
          .willReturn(places);

      // when: 랜덤 장소 조회
      List<PlaceResponse> result = placeQueryService.findRandomPlaces(address, themes, limit);

      // then: 주소로만 필터링됨
      then(placeRepository).should(times(1)).findIdsByAddressStartingWithAndThemes(address, themes);

      // then: 결과 반환됨
      assertThat(result).hasSize(3);
    }
  }
}
