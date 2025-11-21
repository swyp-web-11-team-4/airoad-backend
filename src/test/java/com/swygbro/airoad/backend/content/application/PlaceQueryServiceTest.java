package com.swygbro.airoad.backend.content.application;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.domain.dto.response.PlaceResponse;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.fixture.content.PlaceFixture;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PlaceQueryServiceTest {

  @Mock private PlaceRepository placeRepository;

  @InjectMocks private PlaceQueryService placeQueryService;

  @Nested
  @DisplayName("장소 이름으로 장소 조회 요청")
  class FindPlaceByName {

    @Test
    @DisplayName("요청한 이름의 장소가 존재하면 장소 정보를 반환한다.")
    void shouldReturnPlaceWhenNameExists() {
      // given
      Place place = PlaceFixture.createGangnam();

      String placeName = place.getLocation().getName();
      when(placeRepository.findByName(placeName)).thenReturn(Optional.of(place));

      // when
      PlaceResponse response = placeQueryService.findPlaceByName(placeName);

      // then
      assertThat(response.name()).isEqualTo(placeName);
      assertThat(response.address()).isEqualTo(place.getLocation().getAddress());
      assertThat(response.latitude()).isEqualTo(place.getLocation().getPoint().getY());
      assertThat(response.longitude()).isEqualTo(place.getLocation().getPoint().getX());
      assertThat(response.description()).isEqualTo(place.getDescription());
      assertThat(response.isMustVisit()).isEqualTo(place.getIsMustVisit());
    }

    @Test
    @DisplayName("요청한 이름의 장소가 존재하지 않으면 예외가 발생한다.")
    void shouldThrowExceptionWhenNameDoesNotExist() {
      // given
      String placeName = "없는 장소";
      when(placeRepository.findByName(placeName)).thenReturn(Optional.empty());

      // when & then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> placeQueryService.findPlaceByName(placeName));
      assertThat(exception.getErrorCode()).isEqualTo(TripErrorCode.PLACE_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("장소 ID 목록으로 장소 조회 요청")
  class FindAllPlaceById {

    @Test
    @DisplayName("요청한 ID의 장소들이 존재하면 장소 정보 목록을 반환한다.")
    void shouldReturnPlacesWhenIdsExist() {
      // given
      Place place1 = PlaceFixture.withId(1L, PlaceFixture.createGangnam());
      Place place2 = PlaceFixture.withId(2L, PlaceFixture.createJejuAirport());
      List<Place> places = List.of(place1, place2);
      List<Long> placeIds = List.of(1L, 2L);

      when(placeRepository.findAllByIds(placeIds)).thenReturn(places);

      // when
      List<PlaceResponse> responses = placeQueryService.findAllPlaceById(placeIds);

      // then
      assertThat(responses).hasSize(2);
      assertThat(responses.get(0).name()).isEqualTo(place1.getLocation().getName());
      assertThat(responses.get(1).name()).isEqualTo(place2.getLocation().getName());
    }

    @Test
    @DisplayName("요청한 ID의 장소가 하나도 존재하지 않으면 예외가 발생한다.")
    void shouldThrowExceptionWhenIdsDoNotExist() {
      // given
      List<Long> placeIds = List.of(1L, 2L);
      when(placeRepository.findAllByIds(placeIds)).thenReturn(Collections.emptyList());

      // when & then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> placeQueryService.findAllPlaceById(placeIds));
      assertThat(exception.getErrorCode()).isEqualTo(TripErrorCode.PLACE_NOT_FOUND);
    }
  }
}
