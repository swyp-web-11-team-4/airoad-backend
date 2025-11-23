package com.swygbro.airoad.backend.content.domain.vo;

/** 두 장소 간의 거리 및 예상 이동 시간을 나타내는 값 객체 */
public record Distance(double straightlineDistanceKm, int estimatedMinutes) {
  public Distance {
    if (straightlineDistanceKm < 0) {
      throw new IllegalArgumentException("거리는 0km 이상이어야 합니다");
    }
    if (estimatedMinutes < 0) {
      throw new IllegalArgumentException("예상 이동 시간은 0분 이상이어야 합니다");
    }
  }
}
