package com.swygbro.airoad.backend.content.application;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.swygbro.airoad.backend.content.domain.dto.response.PlaceResponse;
import com.swygbro.airoad.backend.content.domain.vo.Coordinate;
import com.swygbro.airoad.backend.content.domain.vo.Distance;

import lombok.extern.slf4j.Slf4j;

/** 거리 계산 기능을 구현하는 서비스 클래스 */
@Slf4j
@Service
public class DistanceCalculationService implements DistanceCalculationUseCase {

  private static final double TORTUOSITY_FACTOR = 1.8;
  private static final double AVERAGE_SPEED_KMPH = 25.0;

  @Override
  public Distance calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    double straightDistanceKm = calculateHaversine(lat1, lon1, lat2, lon2);
    int estimatedMinutes = calculateEstimatedMinutes(straightDistanceKm);
    return new Distance(straightDistanceKm, estimatedMinutes);
  }

  @Override
  public List<Distance> calculateRouteDistances(List<Coordinate> coordinates) {
    List<Distance> distances = new ArrayList<>();
    if (coordinates == null || coordinates.size() < 2) {
      return distances;
    }

    for (int i = 0; i < coordinates.size() - 1; i++) {
      Coordinate current = coordinates.get(i);
      Coordinate next = coordinates.get(i + 1);

      if (current != null && next != null) {
        distances.add(
            calculateDistance(
                current.latitude(), current.longitude(), next.latitude(), next.longitude()));
      } else {
        log.warn("유효하지 않은 좌표(null)가 포함되어 있어 거리를 0으로 처리합니다: {} -> {}", current, next);
        distances.add(new Distance(0.0, 0));
      }
    }
    return distances;
  }

  private double calculateHaversine(double lat1, double lon1, double lat2, double lon2) {
    final int R = 6371;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  private int calculateEstimatedMinutes(double straightDistanceKm) {
    double roadDistance = straightDistanceKm * TORTUOSITY_FACTOR;
    double hours = roadDistance / AVERAGE_SPEED_KMPH;
    return (int) Math.round(hours * 60);
  }

  private void validateCoordinates(PlaceResponse from, PlaceResponse to) {
    if (from == null || from.latitude() == null || from.longitude() == null) {
      throw new IllegalArgumentException("출발지 좌표가 없습니다");
    }
    if (to == null || to.latitude() == null || to.longitude() == null) {
      throw new IllegalArgumentException("도착지 좌표가 없습니다");
    }
  }
}
