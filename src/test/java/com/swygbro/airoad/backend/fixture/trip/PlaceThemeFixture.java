package com.swygbro.airoad.backend.fixture.trip;

import java.util.ArrayList;
import java.util.List;

import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.domain.entity.PlaceTheme;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
import com.swygbro.airoad.backend.fixture.content.PlaceFixture;

public class PlaceThemeFixture {

  public static PlaceTheme create() {
    return PlaceTheme.builder()
        .place(PlaceFixture.create())
        .placeThemeType(PlaceThemeType.HEALING)
        .build();
  }

  public static PlaceTheme createActivity() {
    return PlaceTheme.builder()
        .place(PlaceFixture.create())
        .placeThemeType(PlaceThemeType.EXPERIENCE_ACTIVITY)
        .build();
  }

  public static PlaceTheme createFoodTour() {
    return PlaceTheme.builder()
        .place(PlaceFixture.create())
        .placeThemeType(PlaceThemeType.RESTAURANT)
        .build();
  }

  public static PlaceTheme createCulture() {
    return PlaceTheme.builder()
        .place(PlaceFixture.create())
        .placeThemeType(PlaceThemeType.CULTURE_ART)
        .build();
  }

  public static List<PlaceTheme> createWithPlaces(Place place) {
    List<PlaceTheme> placeThemes = new ArrayList<>();
    for (PlaceThemeType themeType : place.getThemes()) {
      PlaceTheme placeTheme = PlaceTheme.builder().place(place).placeThemeType(themeType).build();
      placeThemes.add(placeTheme);
    }
    return placeThemes;
  }

  public static PlaceTheme.PlaceThemeBuilder builder() {
    return PlaceTheme.builder().place(PlaceFixture.create()).placeThemeType(PlaceThemeType.HEALING);
  }
}
