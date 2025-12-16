package com.swygbro.airoad.backend.content.application;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.swygbro.airoad.backend.content.domain.vo.Coordinate;
import com.swygbro.airoad.backend.content.domain.vo.Distance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("거리 계산 기능")
class DistanceCalculationServiceTest {

  @InjectMocks private DistanceCalculationService distanceCalculationService;

  @Nested
  @DisplayName("좌표로 거리를 조회하면")
  class WhenCalculateDistanceByCoordinates {

    @Test
    @DisplayName("올바른 직선거리와 예상 이동시간이 반환되어야 한다")
    void shouldReturnValidDistanceAndTime() {
      // given
      // 서울역(126.9716, 37.5547) -> 강남역(127.0276, 37.4979)
      double lat1 = 37.5547;
      double lon1 = 126.9716;
      double lat2 = 37.4979;
      double lon2 = 127.0276;

      // when
      Distance result = distanceCalculationService.calculateDistance(lat1, lon1, lat2, lon2);

      // then
      assertThat(result).isNotNull();
      assertThat(result.straightlineDistanceKm()).isGreaterThan(0);
      assertThat(result.straightlineDistanceKm()).isLessThan(10);
      assertThat(result.estimatedMinutes()).isGreaterThan(0);
    }

    @Test
    @DisplayName("출발지와 도착지가 같으면 거리는 0이어야 한다")
    void shouldReturnZeroDistanceWhenSameCoordinates() {
      // given
      double lat = 37.5547;
      double lon = 126.9716;

      // when
      Distance result = distanceCalculationService.calculateDistance(lat, lon, lat, lon);

      // then
      assertThat(result.straightlineDistanceKm()).isLessThan(0.01);
      assertThat(result.estimatedMinutes()).isZero();
    }

    @Test
    @DisplayName("장거리 조회 시 예상 이동시간이 정확히 계산되어야 한다")
    void shouldCalculateEstimatedTimeAccuratelyForLongDistance() {
      // given
      // 서울역 -> 인천공항 (약 60km)
      double lat1 = 37.5547;
      double lon1 = 126.9716;
      double lat2 = 37.4602;
      double lon2 = 126.4407;

      // when
      Distance result = distanceCalculationService.calculateDistance(lat1, lon1, lat2, lon2);

      // then
      assertThat(result.estimatedMinutes()).isGreaterThan(0);
      // 대략 60km * 1.8 (도로굴곡) / 25 km/h * 60분 = 약 259분
      assertThat(result.estimatedMinutes()).isGreaterThan(200);
    }
  }

  @Nested
  @DisplayName("경로 거리 계산 시")
  class WhenCalculateRouteDistances {

    @Test
    @DisplayName("여러 좌표 목록에 대해 구간별 거리를 계산한다")
    void shouldCalculateDistancesForMultipleCoordinates() {
      // given
      List<Coordinate> coordinates =
          List.of(
              new Coordinate(37.5547, 126.9716), // 서울역
              new Coordinate(37.4979, 127.0276), // 강남역
              new Coordinate(37.4602, 126.4407) // 인천공항
              );

      // when
      List<Distance> distances = distanceCalculationService.calculateRouteDistances(coordinates);

      // then
      assertThat(distances).hasSize(2); // 구간은 2개
      assertThat(distances.get(0).straightlineDistanceKm()).isGreaterThan(0);
      assertThat(distances.get(1).straightlineDistanceKm()).isGreaterThan(0);
    }

    @Test
    @DisplayName("좌표가 2개 미만이면 빈 리스트를 반환한다")
    void shouldReturnEmptyListForInsufficientCoordinates() {
      // given
      List<Coordinate> singleList = List.of(new Coordinate(37.0, 127.0));

      // when
      List<Distance> result1 = distanceCalculationService.calculateRouteDistances(singleList);
      List<Distance> result2 = distanceCalculationService.calculateRouteDistances(null);

      // then
      assertThat(result1).isEmpty();
      assertThat(result2).isEmpty();
    }
  }

  @Nested
  @DisplayName("이동거리 값 객체를 생성하면")
  class WhenCreateDistance {

    @Test
    @DisplayName("음수 거리로는 생성되지 않아야 한다")
    void shouldNotCreateWithNegativeDistance() {
      // when & then
      assertThatThrownBy(() -> new Distance(-1.0, 10))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("거리");
    }

    @Test
    @DisplayName("음수 이동시간으로는 생성되지 않아야 한다")
    void shouldNotCreateWithNegativeMinutes() {
      // when & then
      assertThatThrownBy(() -> new Distance(5.0, -1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("시간");
    }

    @Test
    @DisplayName("0 값으로는 생성될 수 있어야 한다")
    void shouldCreateWithZeroValues() {
      // when
      Distance result = new Distance(0, 0);

      // then
      assertThat(result.straightlineDistanceKm()).isZero();
      assertThat(result.estimatedMinutes()).isZero();
    }
  }
}
