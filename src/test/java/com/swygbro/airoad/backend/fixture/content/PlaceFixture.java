package com.swygbro.airoad.backend.fixture.content;

import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.fixture.common.LocationFixture;

public class PlaceFixture {

  public static Place create() {
    return Place.builder()
        .location(LocationFixture.create())
        .description("서울의 대표적인 교통 허브이자 역사적인 랜드마크입니다. 다양한 맛집과 쇼핑 시설이 있어 관광객들에게 인기가 많습니다.")
        .imageUrl("https://example.com/seoul-station.jpg")
        .operatingHours("00:00-24:00")
        .holidayInfo("연중무휴")
        .isMustVisit(false)
        .placeScore(3)
        .build();
  }

  public static Place createMustVisit() {
    return Place.builder()
        .location(LocationFixture.createGangnam())
        .description("서울의 중심지이자 쇼핑과 엔터테인먼트의 메카입니다. K-pop 문화를 체험할 수 있는 명소입니다.")
        .imageUrl("https://example.com/gangnam.jpg")
        .operatingHours("00:00-24:00")
        .holidayInfo("연중무휴")
        .isMustVisit(true)
        .placeScore(5)
        .build();
  }

  public static Place createTouristSpot() {
    return Place.builder()
        .location(LocationFixture.createJejuAirport())
        .description("제주도의 관문인 국제공항입니다. 제주 여행의 시작점으로 다양한 렌터카와 관광 서비스를 이용할 수 있습니다.")
        .imageUrl("https://example.com/jeju-airport.jpg")
        .operatingHours("00:00-24:00")
        .holidayInfo("연중무휴")
        .isMustVisit(false)
        .placeScore(4)
        .build();
  }

  public static Place createRestaurant() {
    return Place.builder()
        .location(
            LocationFixture.builder()
                .name("맛집 테스트")
                .address("서울특별시 종로구 맛집로 123")
                .point(LocationFixture.createPoint(126.9784, 37.5701))
                .build())
        .description("전통 한식을 현대적으로 재해석한 레스토랑입니다. 현지인들에게 인기가 많은 숨은 맛집입니다.")
        .imageUrl("https://example.com/restaurant.jpg")
        .operatingHours("11:00-22:00")
        .holidayInfo("매주 일요일 휴무")
        .isMustVisit(false)
        .placeScore(4)
        .build();
  }

  public static Place.PlaceBuilder builder() {
    return Place.builder()
        .location(LocationFixture.create())
        .description("테스트용 장소 설명입니다.")
        .imageUrl("https://example.com/place.jpg")
        .operatingHours("09:00-18:00")
        .holidayInfo("매주 월요일 휴무")
        .isMustVisit(false)
        .placeScore(3);
  }
}
