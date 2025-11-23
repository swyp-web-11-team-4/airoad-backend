package com.swygbro.airoad.backend.content.application;

import com.swygbro.airoad.backend.content.domain.vo.Distance;

/** 거리 계산 비즈니스 로직을 정의하는 UseCase 인터페이스 */
public interface DistanceCalculationUseCase {

  /**
   * 두 좌표 간의 거리와 예상 이동 시간을 계산합니다.
   *
   * @param lat1 출발지 위도
   * @param lon1 출발지 경도
   * @param lat2 도착지 위도
   * @param lon2 도착지 경도
   * @return 거리 및 예상 이동 시간 정보
   */
  Distance calculateDistance(double lat1, double lon1, double lat2, double lon2);
}
