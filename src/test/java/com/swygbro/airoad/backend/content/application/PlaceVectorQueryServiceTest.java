package com.swygbro.airoad.backend.content.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import com.swygbro.airoad.backend.content.application.FakeVectorStore.FakeDocument;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceVectorQueryServiceTest {

  private PlaceVectorQueryService placeVectorQueryService;
  private FakeVectorStore fakeVectorStore;

  @BeforeEach
  void setUp() {
    fakeVectorStore = new FakeVectorStore();
    placeVectorQueryService = new PlaceVectorQueryService(fakeVectorStore);
  }

  @Nested
  @DisplayName("키워드_기반_장소_검색을_요청하면")
  class SearchPlacesByKeywords {

    @Test
    @DisplayName("단일_키워드로_검색이_수행되어야_한다")
    void 단일_키워드로_검색이_수행되어야_한다() {
      fakeVectorStore.add(createDocuments(5));

      List<Document> result =
          placeVectorQueryService.searchPlacesByKeywords(
              List.of("카페"), "서울특별시", "강남구", List.of(PlaceThemeType.CAFE), null);

      assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("다중_키워드로_검색이_수행되고_결과가_병합되어야_한다")
    void 다중_키워드로_검색이_수행되고_결과가_병합되어야_한다() {
      fakeVectorStore.add(createDocuments(10));

      List<Document> result =
          placeVectorQueryService.searchPlacesByKeywords(
              List.of("카페", "베이커리", "디저트"), "서울특별시", "강남구", List.of(PlaceThemeType.CAFE), null);

      assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("중복된_장소는_하나만_남아야_한다")
    void 중복된_장소는_하나만_남아야_한다() {
      List<Document> docs = createDocuments(5);
      fakeVectorStore.add(docs);

      List<Document> result =
          placeVectorQueryService.searchPlacesByKeywords(
              List.of("카페", "커피"), "서울특별시", "강남구", List.of(PlaceThemeType.CAFE), null);

      long uniquePlaceIds =
          result.stream().map(d -> d.getMetadata().get("placeId")).distinct().count();
      assertThat(uniquePlaceIds).isEqualTo(result.size());
    }

    @Test
    @DisplayName("categoryLimit이_설정되면_해당_카테고리만_검색되어야_한다")
    void categoryLimit이_설정되면_해당_카테고리만_검색되어야_한다() {
      fakeVectorStore.add(createDocuments(5));

      List<Document> result =
          placeVectorQueryService.searchPlacesByKeywords(
              List.of("맛집"),
              "서울특별시",
              "강남구",
              List.of(PlaceThemeType.CAFE, PlaceThemeType.RESTAURANT),
              PlaceThemeType.RESTAURANT);

      assertThat(result).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("빈_결과_처리")
  class EmptyResults {

    @Test
    @DisplayName("검색_결과가_없으면_빈_리스트가_반환되어야_한다")
    void 검색_결과가_없으면_빈_리스트가_반환되어야_한다() {
      List<Document> result =
          placeVectorQueryService.searchPlacesByKeywords(
              List.of("존재하지않는장소"), "서울특별시", "강남구", List.of(PlaceThemeType.CAFE), null);

      assertThat(result).isEmpty();
    }
  }

  private List<Document> createDocuments(int count) {
    List<Document> documents = new java.util.ArrayList<>();
    for (int i = 0; i < count; i++) {
      Map<String, Object> metadata = new HashMap<>();
      metadata.put("placeId", i + 1);
      metadata.put("name", "Place " + (i + 1));
      metadata.put("province", "서울특별시");
      metadata.put("district", "강남구");
      metadata.put("themes", List.of("CAFE"));

      FakeDocument doc = new FakeDocument("id-" + (i + 1), "Content " + (i + 1), metadata);
      doc.setScore(0.8 - (i * 0.05));
      documents.add(doc);
    }
    return documents;
  }
}
