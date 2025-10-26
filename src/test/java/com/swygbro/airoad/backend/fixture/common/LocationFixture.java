package com.swygbro.airoad.backend.fixture.common;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import com.swygbro.airoad.backend.common.domain.embeddable.Location;

public class LocationFixture {

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  public static Location create() {
    return Location.builder()
        .name("서울역")
        .address("서울특별시 용산구 한강대로 405")
        .point(createPoint(126.9716, 37.5547))
        .build();
  }

  public static Location createGangnam() {
    return Location.builder()
        .name("강남역")
        .address("서울특별시 강남구 강남대로 지하 396")
        .point(createPoint(127.0276, 37.4979))
        .build();
  }

  public static Location createJejuAirport() {
    return Location.builder()
        .name("제주국제공항")
        .address("제주특별자치도 제주시 공항로 2")
        .point(createPoint(126.4930, 33.5111))
        .build();
  }

  public static Location.LocationBuilder builder() {
    return Location.builder()
        .name("테스트 장소")
        .address("서울특별시 강남구 테스트로 123")
        .point(createPoint(127.0, 37.5));
  }

  /**
   * 경도(longitude)와 위도(latitude)를 받아 JTS Point 객체를 생성합니다.
   *
   * @param longitude 경도 (X 좌표)
   * @param latitude 위도 (Y 좌표)
   * @return JTS Point 객체
   */
  public static Point createPoint(double longitude, double latitude) {
    return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
  }
}
