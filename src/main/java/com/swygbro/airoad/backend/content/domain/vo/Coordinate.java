package com.swygbro.airoad.backend.content.domain.vo;

/**
 * 위도와 경도를 나타내는 값 객체
 *
 * @param latitude 위도 (-90 ~ 90)
 * @param longitude 경도 (-180 ~ 180)
 */
public record Coordinate(Double latitude, Double longitude) {
  public Coordinate {
    if (latitude == null || longitude == null) {
      throw new IllegalArgumentException("위도와 경도는 필수입니다.");
    }
    if (latitude < -90 || latitude > 90) {
      throw new IllegalArgumentException("위도는 -90도에서 90도 사이여야 합니다. 입력값: " + latitude);
    }
    if (longitude < -180 || longitude > 180) {
      throw new IllegalArgumentException("경도는 -180도에서 180도 사이여야 합니다. 입력값: " + longitude);
    }
  }
}
