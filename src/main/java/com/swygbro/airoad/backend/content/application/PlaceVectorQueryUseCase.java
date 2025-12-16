package com.swygbro.airoad.backend.content.application;

import java.util.List;

import org.springframework.ai.document.Document;

import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;

public interface PlaceVectorQueryUseCase {

  List<Document> search(String query, int topK, double similarityThreshold);

  /**
   * 키워드 목록과 필터 조건을 기반으로 장소를 검색하고 중복을 제거합니다.
   *
   * @param keywords 검색할 키워드 리스트
   * @param province 광역자치단체 (필터 조건)
   * @param district 시/군/구 (필터 조건)
   * @param themes 테마 리스트 (필터 조건)
   * @param categoryLimit 특정 카테고리만 검색할 경우 (예: RESTAURANT), null이면 테마 필터 적용
   * @return 검색된 장소 목록 (Document)
   */
  List<Document> searchPlacesByKeywords(
      List<String> keywords,
      String province,
      String district,
      List<PlaceThemeType> themes,
      PlaceThemeType categoryLimit);
}
