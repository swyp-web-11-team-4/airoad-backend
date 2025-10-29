package com.swygbro.airoad.backend.fixture.trip;

import com.swygbro.airoad.backend.trip.domain.entity.PlaceTheme;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.domain.entity.TripTheme;

public class TripThemeFixture {

  public static TripTheme create() {
    return TripTheme.builder().placeTheme(PlaceThemeFixture.create()).priority(1).build();
  }

  public static TripTheme createActivity() {
    return TripTheme.builder().placeTheme(PlaceThemeFixture.createActivity()).priority(1).build();
  }

  public static TripTheme createFoodTour() {
    return TripTheme.builder().placeTheme(PlaceThemeFixture.createFoodTour()).priority(2).build();
  }

  public static TripTheme createCulture() {
    return TripTheme.builder().placeTheme(PlaceThemeFixture.createCulture()).priority(3).build();
  }

  public static TripTheme createWithPlaceTheme(PlaceTheme placeTheme) {
    return TripTheme.builder().placeTheme(placeTheme).priority(1).build();
  }

  public static TripTheme createWithTripPlan(TripPlan tripPlan, PlaceTheme placeTheme) {
    return TripTheme.builder().tripPlan(tripPlan).placeTheme(placeTheme).priority(1).build();
  }

  public static TripTheme.TripThemeBuilder builder() {
    return TripTheme.builder().placeTheme(PlaceThemeFixture.create()).priority(1);
  }
}
