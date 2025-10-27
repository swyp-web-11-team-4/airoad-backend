package com.swygbro.airoad.backend.fixture.trip;

import java.util.Arrays;
import java.util.List;

import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.fixture.content.PlaceFixture;
import com.swygbro.airoad.backend.trip.domain.entity.PlaceTheme;

public class PlaceThemeFixture {

  public static PlaceTheme create() {
    return PlaceTheme.builder()
        .places(Arrays.asList(PlaceFixture.create()))
        .themeName("힐링")
        .build();
  }

  public static PlaceTheme createActivity() {
    return PlaceTheme.builder()
        .places(Arrays.asList(PlaceFixture.createTouristSpot()))
        .themeName("액티비티")
        .build();
  }

  public static PlaceTheme createFoodTour() {
    return PlaceTheme.builder()
        .places(Arrays.asList(PlaceFixture.createRestaurant()))
        .themeName("맛집 투어")
        .build();
  }

  public static PlaceTheme createCulture() {
    return PlaceTheme.builder()
        .places(Arrays.asList(PlaceFixture.createMustVisit()))
        .themeName("문화/역사")
        .build();
  }

  public static PlaceTheme createWithPlaces(List<Place> places) {
    return PlaceTheme.builder().places(places).themeName("테스트 테마").build();
  }

  public static PlaceTheme.PlaceThemeBuilder builder() {
    return PlaceTheme.builder().places(Arrays.asList(PlaceFixture.create())).themeName("테스트 테마");
  }
}
