package com.swygbro.airoad.backend.content.application;

import java.util.List;

import com.swygbro.airoad.backend.content.domain.dto.response.PlaceResponse;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;

public interface PlaceQueryUseCase {

  /**
   * 지역과 테마를 기반으로 랜덤 장소를 추천합니다
   *
   * @param province 시/도 (예: "강원도", "서울특별시")
   * @param themes 테마 리스트 (예: ["음식점", "힐링"])
   * @param limit 추천할 장소 개수
   * @return 랜덤하게 선택된 장소 목록
   */
  List<PlaceResponse> findRandomPlaces(String province, List<PlaceThemeType> themes, int limit);
}
