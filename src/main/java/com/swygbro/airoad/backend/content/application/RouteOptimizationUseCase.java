package com.swygbro.airoad.backend.content.application;

import java.util.List;

import org.springframework.ai.document.Document;

public interface RouteOptimizationUseCase {

  /**
   * 후보 장소와 음식점을 기반으로 최적의 경로를 생성합니다.
   *
   * @param places 관광지 후보군
   * @param restaurants 음식점 후보군
   * @return 최적화된 순서의 장소 리스트
   */
  List<Document> optimizeRoute(List<Document> places, List<Document> restaurants);
}
