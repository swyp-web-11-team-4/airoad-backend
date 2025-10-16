package com.swygbro.airoad.backend.trip.domain.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.swygbro.airoad.backend.trip.domain.entity.Transportation;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 장소 간 이동 정보를 나타내는 Embeddable 객체 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelSegment {

  /** 예상 이동 시간 (분) */
  private Integer travelTime;

  /** 이동 수단 */
  @Enumerated(EnumType.STRING)
  private Transportation transportation;

  @Builder
  public TravelSegment(Integer travelTime, Transportation transportation) {
    this.travelTime = travelTime;
    this.transportation = transportation;
  }
}
