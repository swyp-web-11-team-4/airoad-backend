package com.swygbro.airoad.backend.fixture.trip;

import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.fixture.content.PlaceFixture;
import com.swygbro.airoad.backend.trip.domain.entity.TripTheme;

public class TripThemeFixture {

  public static TripTheme create() {
    return TripTheme.builder().place(PlaceFixture.create()).themeName("힐링").build();
  }

  public static TripTheme createActivity() {
    return TripTheme.builder().place(PlaceFixture.createTouristSpot()).themeName("액티비티").build();
  }

  public static TripTheme createFoodTour() {
    return TripTheme.builder().place(PlaceFixture.createRestaurant()).themeName("맛집 투어").build();
  }

  public static TripTheme createCulture() {
    return TripTheme.builder().place(PlaceFixture.createMustVisit()).themeName("문화/역사").build();
  }

  public static TripTheme createWithPlace(Place place) {
    return TripTheme.builder().place(place).themeName("테스트 테마").build();
  }

  public static TripTheme.TripThemeBuilder builder() {
    return TripTheme.builder().place(PlaceFixture.create()).themeName("테스트 테마");
  }
}
