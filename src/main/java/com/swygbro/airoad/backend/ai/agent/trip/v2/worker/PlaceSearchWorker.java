package com.swygbro.airoad.backend.ai.agent.trip.v2.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.common.worker.Worker;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.KeywordResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.PlaceSearchResponse;
import com.swygbro.airoad.backend.ai.agent.trip.v2.context.TripPlanContext;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class PlaceSearchWorker implements Worker<TripPlanContext, TripPlanTaskType> {

  private final VectorStore vectorStore;

  @Override
  public TripPlanTaskType getTaskType() {
    return TripPlanTaskType.VECTOR_SEARCH;
  }

  @Override
  public void execute(TripPlanContext context) {
    KeywordResponse keywords = context.getKeywords();

    List<PlaceThemeType> userThemes = context.getRequest().themes();

    if (keywords == null) {
      log.warn("키워드가 없어 검색을 중단합니다.");
      return;
    }

    List<Document> places = vectorSearch(keywords.places(), createThemeFilter(userThemes));
    List<Document> restaurants =
        vectorSearch(keywords.restaurants(), createThemeFilter(List.of(PlaceThemeType.RESTAURANT)));

    log.info("검색 완료: 총 {}개 장소, {}개 음식점 조회", places.size(), restaurants.size());

    List<Long> previousPlaceIds = context.getPreviousPlaceIds();

    List<Document> deduplicatedPlaces =
        places.stream()
            .filter(
                place -> {
                  Long placeId = ((Integer) place.getMetadata().get("placeId")).longValue();
                  return !previousPlaceIds.contains(placeId);
                })
            .distinct()
            .toList();

    List<Document> deduplicatedRestaurants =
        restaurants.stream()
            .filter(
                restaurant -> {
                  Long placeId = ((Integer) restaurant.getMetadata().get("placeId")).longValue();
                  return !previousPlaceIds.contains(placeId);
                })
            .distinct()
            .toList();

    log.info(
        "중복 제거 완료: {}개 장소, {}개 음식점", deduplicatedPlaces.size(), deduplicatedRestaurants.size());

    PlaceSearchResponse searchResponse =
        PlaceSearchResponse.builder()
            .places(deduplicatedPlaces)
            .restaurants(deduplicatedRestaurants)
            .build();

    context.setSearchResults(searchResponse);
  }

  /**
   * 벡터 검색 실행
   *
   * @param keywords 검색어
   * @param filter 적용할 테마 필터
   */
  private List<Document> vectorSearch(List<String> keywords, Filter.Expression filter) {
    List<Document> documents = new ArrayList<>();

    for (String keyword : keywords) {
      documents.addAll(
          vectorStore
              .similaritySearch(
                  SearchRequest.builder()
                      .query(keyword)
                      .topK(4)
                      .similarityThreshold(0.45d)
                      .filterExpression(filter)
                      .build())
              .stream()
              .distinct()
              .toList());
    }

    return documents;
  }

  private Filter.Expression createThemeFilter(List<PlaceThemeType> userThemes) {
    List<String> allowedThemes = userThemes.stream().map(PlaceThemeType::getDescription).toList();

    String filterExpressionStr =
        allowedThemes.stream()
            .map(theme -> "themes == '" + theme + "'")
            .collect(Collectors.joining(" || "));

    log.info("생성된 필터 문자열: {}", filterExpressionStr);

    if (filterExpressionStr.isEmpty()) {
      return null;
    }

    return new FilterExpressionTextParser().parse(filterExpressionStr);
  }
}
