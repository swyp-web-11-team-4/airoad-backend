package com.swygbro.airoad.backend.ai.agent.trip.step;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.trip.context.ExecutionContext;
import com.swygbro.airoad.backend.ai.agent.trip.context.TripPlanContextKey;
import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.KeywordResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.PlaceSearchResponse;
import com.swygbro.airoad.backend.ai.agent.trip.pipeline.PipelineStep;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 파이프라인 2단계: 벡터 검색으로 장소 탐색
 *
 * <p>v2의 PlaceSearchWorker에서 중복 제거 로직을 분리 SRP: 순수 검색만 담당 (중복 제거는 DeduplicationStep으로)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceSearchStep implements PipelineStep {

  private final VectorStore vectorStore;

  @Override
  public ExecutionContext execute(ExecutionContext context) {
    KeywordResponse keywords = context.get(TripPlanContextKey.KEYWORDS);
    AiDailyPlanRequest request = context.get(TripPlanContextKey.REQUEST);
    String selectedProvince = keywords.selectedProvince().getOfficialName();
    String selectedDistrict = keywords.selectedDistrict();

    List<PlaceThemeType> userThemes = request.themes();

    Filter.Expression placeFilter =
        combineFilterStrings(
            createDistrictFilterStr(selectedProvince, selectedDistrict),
            createThemeFilterStr(userThemes));
    List<Document> places = vectorSearch(keywords.places(), placeFilter);

    Filter.Expression restaurantFilter =
        combineFilterStrings(
            createDistrictFilterStr(selectedProvince, selectedDistrict),
            createThemeFilterStr(List.of(PlaceThemeType.RESTAURANT)));
    List<Document> restaurants = vectorSearch(keywords.restaurants(), restaurantFilter);

    log.info("검색 완료: 총 {}개 장소, {}개 음식점 조회", places.size(), restaurants.size());

    PlaceSearchResponse searchResult = PlaceSearchResponse.of(places, restaurants);
    context.put(TripPlanContextKey.SEARCH_RESULTS, searchResult);

    return context;
  }

  @Override
  public String getName() {
    return "PlaceSearch";
  }

  private List<Document> vectorSearch(List<String> keywords, Filter.Expression filter) {
    List<Document> documents = new ArrayList<>();

    for (String keyword : keywords) {
      documents.addAll(
          vectorStore
              .similaritySearch(
                  SearchRequest.builder()
                      .query(keyword)
                      .topK(10)
                      .similarityThreshold(0.45d)
                      .filterExpression(filter)
                      .build())
              .stream()
              .toList());
    }

    // placeId 기준 중복 제거 (유사도 점수가 가장 높은 Document 유지)
    return documents.stream()
        .collect(
            Collectors.toMap(
                doc -> ((Integer) doc.getMetadata().get("placeId")).longValue(),
                doc -> doc,
                (existing, replacement) ->
                    existing.getScore() >= replacement.getScore() ? existing : replacement))
        .values()
        .stream()
        .toList();
  }

  private String createDistrictFilterStr(String province, String district) {
    return "(province == '" + province + "') && (district == '" + district + "')";
  }

  private String createThemeFilterStr(List<PlaceThemeType> userThemes) {
    List<String> allowedThemes = userThemes.stream().map(PlaceThemeType::name).toList();

    String filterExpressionStr =
        allowedThemes.stream()
            .map(theme -> "themes == '" + theme + "'")
            .collect(Collectors.joining(" || "));

    log.info("생성된 필터 문자열: {}", filterExpressionStr);

    return filterExpressionStr;
  }

  private Filter.Expression combineFilterStrings(String f1, String f2) {
    if (f1 == null || f1.isEmpty()) return new FilterExpressionTextParser().parse(f2);
    if (f2 == null || f2.isEmpty()) return new FilterExpressionTextParser().parse(f1);

    String combined = String.format("(%s) && (%s)", f1, f2);
    return new FilterExpressionTextParser().parse(combined);
  }
}
