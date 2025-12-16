package com.swygbro.airoad.backend.ai.agent.trip.step;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.trip.context.ExecutionContext;
import com.swygbro.airoad.backend.ai.agent.trip.context.TripPlanContextKey;
import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.KeywordResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.PlaceSearchResponse;
import com.swygbro.airoad.backend.ai.agent.trip.pipeline.PipelineStep;
import com.swygbro.airoad.backend.content.application.PlaceVectorQueryUseCase;
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

  private final PlaceVectorQueryUseCase placeVectorQueryUseCase;

  @Override
  public ExecutionContext execute(ExecutionContext context) {
    KeywordResponse keywords = context.get(TripPlanContextKey.KEYWORDS);
    AiDailyPlanRequest request = context.get(TripPlanContextKey.REQUEST);
    String selectedProvince = keywords.selectedProvince().getOfficialName();
    String selectedDistrict = keywords.selectedDistrict();

    List<PlaceThemeType> userThemes = request.themes();

    List<Document> places =
        placeVectorQueryUseCase.searchPlacesByKeywords(
            keywords.places(), selectedProvince, selectedDistrict, userThemes, null);

    List<Document> restaurants =
        placeVectorQueryUseCase.searchPlacesByKeywords(
            keywords.restaurants(),
            selectedProvince,
            selectedDistrict,
            userThemes,
            PlaceThemeType.RESTAURANT);

    log.info("검색 완료: 총 {}개 장소, {}개 음식점 조회", places.size(), restaurants.size());

    PlaceSearchResponse searchResult = PlaceSearchResponse.of(places, restaurants);
    context.put(TripPlanContextKey.SEARCH_RESULTS, searchResult);

    return context;
  }

  @Override
  public String getName() {
    return "PlaceSearch";
  }
}
