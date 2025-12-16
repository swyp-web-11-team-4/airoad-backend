package com.swygbro.airoad.backend.content.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

import com.swygbro.airoad.backend.content.application.FakeVectorStore.FakeDocument;
import com.swygbro.airoad.backend.content.domain.vo.Distance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RouteOptimizationServiceTest {

  private RouteOptimizationService routeOptimizationService;

  @Mock private DistanceCalculationUseCase distanceCalculationUseCase;

  @BeforeEach
  void setUp() {
    routeOptimizationService = new RouteOptimizationService(distanceCalculationUseCase);
  }

  @Nested
  @DisplayName("경로_최적화를_요청하면")
  class OptimizeRoute {

    @Test
    @DisplayName("빈_관광지_목록이_주어지면_빈_경로가_반환되어야_한다")
    void 빈_관광지_목록이_주어지면_빈_경로가_반환되어야_한다() {
      List<Document> result =
          routeOptimizationService.optimizeRoute(new ArrayList<>(), new ArrayList<>());

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("null_관광지_목록이_주어지면_빈_경로가_반환되어야_한다")
    void null_관광지_목록이_주어지면_빈_경로가_반환되어야_한다() {
      List<Document> result = routeOptimizationService.optimizeRoute(null, new ArrayList<>());

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("정상적인_장소_목록이_주어지면_최적화된_경로가_반환되어야_한다")
    void 정상적인_장소_목록이_주어지면_최적화된_경로가_반환되어야_한다() {
      given(
              distanceCalculationUseCase.calculateDistance(
                  anyDouble(), anyDouble(), anyDouble(), anyDouble()))
          .willReturn(new Distance(5.0, 10));

      List<Document> places = createPlaces(5);
      List<Document> restaurants = createRestaurants(2);

      List<Document> result = routeOptimizationService.optimizeRoute(places, restaurants);

      assertThat(result).isNotEmpty();
      assertThat(result.size()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("가장_높은_score를_가진_장소가_첫번째에_위치해야_한다")
    void 가장_높은_score를_가진_장소가_첫번째에_위치해야_한다() {
      given(
              distanceCalculationUseCase.calculateDistance(
                  anyDouble(), anyDouble(), anyDouble(), anyDouble()))
          .willReturn(new Distance(5.0, 10));

      List<Document> places = createPlaces(5);

      List<Document> result = routeOptimizationService.optimizeRoute(places, new ArrayList<>());

      assertThat(result.get(0).getMetadata().get("placeId")).isEqualTo(1);
    }

    @Test
    @DisplayName("식당이_있으면_경로에_포함되어야_한다")
    void 식당이_있으면_경로에_포함되어야_한다() {
      given(
              distanceCalculationUseCase.calculateDistance(
                  anyDouble(), anyDouble(), anyDouble(), anyDouble()))
          .willReturn(new Distance(5.0, 10));

      List<Document> places = createPlaces(5);
      List<Document> restaurants = createRestaurants(2);

      List<Document> result = routeOptimizationService.optimizeRoute(places, restaurants);

      long restaurantCount = result.stream().filter(doc -> isRestaurant(doc)).count();
      assertThat(restaurantCount).isGreaterThan(0);
    }
  }

  private List<Document> createPlaces(int count) {
    List<Document> places = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Map<String, Object> metadata = new HashMap<>();
      metadata.put("placeId", i + 1);
      metadata.put("name", "Place " + (i + 1));
      metadata.put("latitude", 37.5 + (i * 0.01));
      metadata.put("longitude", 127.0 + (i * 0.01));
      metadata.put("themes", List.of("FAMOUS_SPOT"));

      FakeDocument doc = new FakeDocument("id-" + (i + 1), "Content " + (i + 1), metadata);
      doc.setScore(0.9 - (i * 0.05));
      places.add(doc);
    }
    return places;
  }

  private List<Document> createRestaurants(int count) {
    List<Document> restaurants = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Map<String, Object> metadata = new HashMap<>();
      metadata.put("placeId", 100 + i);
      metadata.put("name", "Restaurant " + (i + 1));
      metadata.put("latitude", 37.52 + (i * 0.01));
      metadata.put("longitude", 127.02 + (i * 0.01));
      metadata.put("themes", List.of("RESTAURANT"));

      FakeDocument doc = new FakeDocument("id-" + (100 + i), "Restaurant " + (i + 1), metadata);
      doc.setScore(0.85 - (i * 0.05));
      restaurants.add(doc);
    }
    return restaurants;
  }

  private boolean isRestaurant(Document doc) {
    @SuppressWarnings("unchecked")
    List<String> themes = (List<String>) doc.getMetadata().get("themes");
    return themes != null && themes.contains("RESTAURANT");
  }
}
