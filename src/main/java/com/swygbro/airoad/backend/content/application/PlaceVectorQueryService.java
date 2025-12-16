package com.swygbro.airoad.backend.content.application;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.stereotype.Service;

import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceVectorQueryService implements PlaceVectorQueryUseCase {

  private final VectorStore vectorStore;

  @Override
  public List<Document> search(String query, int topK, double similarityThreshold) {
    return vectorStore.similaritySearch(
        SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(similarityThreshold)
            .build());
  }

  @Override
  public List<Document> searchPlacesByKeywords(
      List<String> keywords,
      String province,
      String district,
      List<PlaceThemeType> themes,
      PlaceThemeType categoryLimit) {

    Filter.Expression filter = createFilter(province, district, themes, categoryLimit);
    return vectorSearch(keywords, filter);
  }

  private Filter.Expression createFilter(
      String province, String district, List<PlaceThemeType> themes, PlaceThemeType categoryLimit) {

    String districtFilter = createDistrictFilterStr(province, district);
    String themeFilter;

    if (categoryLimit != null) {
      themeFilter = createThemeFilterStr(List.of(categoryLimit));
    } else {
      themeFilter = createThemeFilterStr(themes);
    }

    return combineFilterStrings(districtFilter, themeFilter);
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

    log.debug("생성된 필터 문자열: {}", filterExpressionStr);

    return filterExpressionStr;
  }

  private Filter.Expression combineFilterStrings(String f1, String f2) {
    if (f1 == null || f1.isEmpty()) return new FilterExpressionTextParser().parse(f2);
    if (f2 == null || f2.isEmpty()) return new FilterExpressionTextParser().parse(f1);

    String combined = String.format("(%s) && (%s)", f1, f2);
    return new FilterExpressionTextParser().parse(combined);
  }
}
