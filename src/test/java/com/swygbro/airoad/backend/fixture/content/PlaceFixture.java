package com.swygbro.airoad.backend.fixture.content;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.fixture.common.LocationFixture;

/** Place 엔티티 테스트 데이터 생성을 위한 Fixture 클래스 */
public class PlaceFixture {

  /**
   * 기본 Place 생성 (필수 필드만)
   *
   * @return 기본 Place 객체
   */
  public static Place create() {
    return Place.builder()
        .location(LocationFixture.create())
        .isMustVisit(false)
        .placeScore(3)
        .build();
  }

  /**
   * 모든 필드가 채워진 Place 생성
   *
   * @return 모든 필드가 채워진 Place 객체
   */
  public static Place createWithFullInfo() {
    return Place.builder()
        .location(LocationFixture.create())
        .description("서울의 중심지에 위치한 역사적인 장소입니다. 다양한 문화 체험과 맛집이 많습니다.")
        .imageUrl("https://example.com/place.jpg")
        .operatingHours("09:00-22:00")
        .holidayInfo("연중무휴")
        .isMustVisit(true)
        .placeScore(5)
        .build();
  }

  /**
   * 필수 필드만 있는 Place 생성 (선택 필드는 null 또는 기본값)
   *
   * @return 필수 필드만 있는 Place 객체
   */
  public static Place createWithMinimalInfo() {
    return Place.builder()
        .location(LocationFixture.createGangnam())
        .description(null)
        .imageUrl(null)
        .operatingHours(null)
        .holidayInfo(null)
        .isMustVisit(false)
        .placeScore(1)
        .build();
  }

  /**
   * 강남역 장소 생성 (테스트 데이터 다양성 확보)
   *
   * @return 강남역 Place 객체
   */
  public static Place createGangnam() {
    return Place.builder()
        .location(LocationFixture.createGangnam())
        .description("서울의 대표적인 상업 지구이자 교통의 요충지입니다.")
        .imageUrl("https://example.com/gangnam.jpg")
        .operatingHours("24시간")
        .holidayInfo("연중무휴")
        .isMustVisit(true)
        .placeScore(4)
        .build();
  }

  /**
   * 제주 공항 장소 생성 (테스트 데이터 다양성 확보)
   *
   * @return 제주공항 Place 객체
   */
  public static Place createJejuAirport() {
    return Place.builder()
        .location(LocationFixture.createJejuAirport())
        .description("제주도 관문, 국제공항")
        .imageUrl("https://example.com/jeju-airport.jpg")
        .operatingHours("24시간")
        .holidayInfo("연중무휴")
        .isMustVisit(false)
        .placeScore(3)
        .build();
  }

  /**
   * 필수 방문지 생성 (isMustVisit=true)
   *
   * @return 필수 방문지 Place 객체
   */
  public static Place createMustVisit() {
    return Place.builder()
        .location(LocationFixture.createGangnam())
        .description("서울의 중심지이자 쇼핑과 엔터테인먼트의 메카입니다. K-pop 문화를 체험할 수 있는 명소입니다.")
        .imageUrl("https://example.com/gangnam.jpg")
        .operatingHours("24시간")
        .holidayInfo("연중무휴")
        .isMustVisit(true)
        .placeScore(5)
        .build();
  }

  /**
   * 레스토랑 장소 생성 (맛집)
   *
   * @return 레스토랑 Place 객체
   */
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

  /**
   * 관광지 장소 생성
   *
   * @return 관광지 Place 객체
   */
  public static Place createTouristSpot() {
    return Place.builder()
        .location(LocationFixture.createJejuAirport())
        .description("제주도의 관문인 국제공항입니다. 제주 여행의 시작점으로 다양한 렌터카와 관광 서비스를 이용할 수 있습니다.")
        .imageUrl("https://example.com/jeju-airport.jpg")
        .operatingHours("24시간")
        .holidayInfo("연중무휴")
        .isMustVisit(false)
        .placeScore(4)
        .build();
  }

  /**
   * 커스텀 Place 생성용 빌더 반환
   *
   * @return Place.PlaceBuilder
   */
  public static Place.PlaceBuilder builder() {
    return Place.builder().location(LocationFixture.create()).isMustVisit(false).placeScore(3);
  }

  /**
   * ID가 설정된 Place 생성 (Reflection 사용)
   *
   * <p>JPA가 자동 생성하는 ID를 테스트에서 설정하기 위해 Reflection을 사용합니다.
   *
   * @param id 설정할 ID
   * @param place ID를 설정할 Place 객체
   * @return ID가 설정된 Place 객체
   */
  public static Place withId(Long id, Place place) {
    try {
      Field idField = place.getClass().getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(place, id);
      return place;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("ID 설정 실패", e);
    }
  }

  /**
   * updatedAt이 설정된 Place 생성 (Reflection 사용)
   *
   * <p>JPA Auditing이 자동 생성하는 updatedAt을 테스트에서 설정하기 위해 Reflection을 사용합니다.
   *
   * @param updatedAt 설정할 수정일시
   * @param place updatedAt을 설정할 Place 객체
   * @return updatedAt이 설정된 Place 객체
   */
  public static Place withUpdatedAt(LocalDateTime updatedAt, Place place) {
    try {
      Field updatedAtField = place.getClass().getSuperclass().getDeclaredField("updatedAt");
      updatedAtField.setAccessible(true);
      updatedAtField.set(place, updatedAt);
      return place;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("updatedAt 설정 실패", e);
    }
  }

  /**
   * createdAt이 설정된 Place 생성 (Reflection 사용)
   *
   * <p>JPA Auditing이 자동 생성하는 createdAt을 테스트에서 설정하기 위해 Reflection을 사용합니다.
   *
   * @param createdAt 설정할 생성일시
   * @param place createdAt을 설정할 Place 객체
   * @return createdAt이 설정된 Place 객체
   */
  public static Place withCreatedAt(LocalDateTime createdAt, Place place) {
    try {
      Field createdAtField = place.getClass().getSuperclass().getDeclaredField("createdAt");
      createdAtField.setAccessible(true);
      createdAtField.set(place, createdAt);
      return place;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("createdAt 설정 실패", e);
    }
  }

  /**
   * ID, createdAt, updatedAt이 모두 설정된 Place 생성 (완전한 엔티티)
   *
   * @param id 설정할 ID
   * @param createdAt 설정할 생성일시
   * @param updatedAt 설정할 수정일시
   * @param place 설정할 Place 객체
   * @return 완전히 초기화된 Place 객체
   */
  public static Place withFullEntity(
      Long id, LocalDateTime createdAt, LocalDateTime updatedAt, Place place) {
    withId(id, place);
    withCreatedAt(createdAt, place);
    withUpdatedAt(updatedAt, place);
    return place;
  }
}
