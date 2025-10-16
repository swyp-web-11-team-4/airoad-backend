package com.swygbro.airoad.backend.common.domain.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import org.locationtech.jts.geom.Point;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 위치 정보를 나타내는 Embeddable 객체 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location {

  /** 장소 이름 */
  @Column(nullable = false)
  private String name;

  /** 주소 */
  @Column(nullable = false)
  private String address;

  /** 좌표 (Point) */
  @Column(nullable = false)
  private Point point;

  @Builder
  public Location(String name, String address, Point point) {
    this.name = name;
    this.address = address;
    this.point = point;
  }
}
