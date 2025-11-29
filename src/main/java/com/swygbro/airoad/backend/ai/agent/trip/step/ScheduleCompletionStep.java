package com.swygbro.airoad.backend.ai.agent.trip.step;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.trip.context.ExecutionContext;
import com.swygbro.airoad.backend.ai.agent.trip.context.TripPlanContextKey;
import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse.AiScheduledPlace;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.DistanceCalculationResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.ScheduleSummaryResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.ScheduleSummaryResponse.PlaceSummary;
import com.swygbro.airoad.backend.ai.agent.trip.pipeline.PipelineStep;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;

import lombok.extern.slf4j.Slf4j;

/**
 * 파이프라인 Step 8: 최종 일정 완성
 *
 * <p>3개 데이터 소스를 통합하여 최종 AiDailyPlanResponse 생성:
 *
 * <ul>
 *   <li>RouteOptimizationResponse: placeId, visitOrder, category
 *   <li>ScheduleSummaryResponse: title, description, summary
 *   <li>DistanceCalculationResponse: travelTime
 * </ul>
 */
@Slf4j
@Component
public class ScheduleCompletionStep implements PipelineStep {

  @Override
  public ExecutionContext execute(ExecutionContext context) {
    // 1. 3개 데이터 소스 조회
    ScheduleSummaryResponse summaryResult = context.get(TripPlanContextKey.SCHEDULE_SUMMARY_RESULT);
    List<DistanceCalculationResponse> distanceResult =
        context.get(TripPlanContextKey.DISTANCE_CALCULATED_PLACES);
    AiDailyPlanRequest request = context.get(TripPlanContextKey.REQUEST);
    Integer dayNumber = context.get(TripPlanContextKey.DAY_NUMBER);

    if (distanceResult == null
        || distanceResult.isEmpty()
        || summaryResult == null
        || summaryResult.placeSummaries() == null) {
      log.warn("일정 완성을 수행할 데이터가 부족합니다.");
      return context;
    }

    // 2. placeId → summary 매핑
    Map<Long, String> summaryMap =
        summaryResult.placeSummaries().stream()
            .collect(Collectors.toMap(PlaceSummary::placeId, PlaceSummary::summary));

    // 3. AiScheduledPlace 조립
    List<AiScheduledPlace> aiScheduledPlaces =
        distanceResult.stream()
            .map(
                d ->
                    AiScheduledPlace.builder()
                        .placeId(d.placeId())
                        .summary(summaryMap.get(d.placeId())) // summary 매칭
                        .visitOrder(d.visitOrder())
                        .category(d.category())
                        .travelTime(d.travelTime())
                        .transportation(Transportation.PUBLIC_TRANSIT)
                        .build())
            .toList();

    // 4. 일정 날짜 계산
    LocalDate scheduleDate = request.startDate().plusDays(dayNumber - 1);

    // 5. AiDailyPlanResponse 생성
    AiDailyPlanResponse dailyPlan =
        AiDailyPlanResponse.builder()
            .dayNumber(dayNumber)
            .date(scheduleDate)
            .title(summaryResult.title())
            .description(summaryResult.description())
            .places(aiScheduledPlaces)
            .build();

    log.info(
        "일정 완성 완료: {}일차 '{}', {}개 장소", dayNumber, summaryResult.title(), aiScheduledPlaces.size());

    context.put(TripPlanContextKey.SCHEDULE, dailyPlan);

    return context;
  }

  @Override
  public String getName() {
    return "ScheduleCompletion";
  }
}
