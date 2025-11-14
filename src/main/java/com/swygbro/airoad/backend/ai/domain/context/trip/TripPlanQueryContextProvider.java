package com.swygbro.airoad.backend.ai.domain.context.trip;

import java.util.List;

import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor.MetadataEntry;
import com.swygbro.airoad.backend.ai.common.context.AbstractContextProvider;
import com.swygbro.airoad.backend.ai.domain.dto.context.TripPlanQueryContext;
import com.swygbro.airoad.backend.trip.application.TripPlanQueryUseCase;
import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.ScheduledPlaceResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanDetailsResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 여행 계획 조회를 위한 Query Context를 제공하는 Provider
 *
 * <p>TripPlanQueryContext로부터 tripPlanId를 추출하여 여행 계획 상세 정보를 조회하고, 요약된 형태로 컨텍스트를 제공합니다.
 *
 * <p>CQRS 패턴의 Query 측면을 담당하며, 기존 여행 계획의 현재 상태를 AI에게 전달합니다.
 *
 * <p>특정 Agent DTO에 의존하지 않아 ChatAgent, TripAgent 등 어디서든 재사용 가능합니다.
 */
@Slf4j
@Component
public class TripPlanQueryContextProvider extends AbstractContextProvider<TripPlanQueryContext> {

  private final TripPlanQueryUseCase tripPlanQueryUseCase;

  public TripPlanQueryContextProvider(TripPlanQueryUseCase tripPlanQueryUseCase) {
    super(TripPlanQueryContext.class);
    this.tripPlanQueryUseCase = tripPlanQueryUseCase;
  }

  @Override
  protected List<MetadataEntry> doGetContext(TripPlanQueryContext context) {
    if (context.tripPlanId() == null) {
      log.debug("tripPlanId가 없어 여행 계획 컨텍스트를 생성하지 않습니다");
      return List.of();
    }

    log.debug("여행 계획 조회 - tripPlanId: {}, username: {}", context.tripPlanId(), context.username());

    TripPlanDetailsResponse tripPlan =
        tripPlanQueryUseCase.findTripPlanDetailsById(context.tripPlanId(), context.username());

    String summary = createTripPlanSummary(tripPlan);

    log.debug("여행 계획 요약 완료 - 길이: {} 자", summary.length());

    return PromptMetadataAdvisor.userMetadata(
        """
        ## 여행 계획 컨텍스트 (Trip Plan Context)

        사용자의 현재 여행 계획 정보입니다.

        %s

        """
            .formatted(summary));
  }

  @Override
  public int getOrder() {
    return 11;
  }

  /**
   * 여행 계획을 요약된 문자열로 변환합니다.
   *
   * @param tripPlan 여행 계획 상세 정보
   * @return 요약된 여행 계획 문자열
   */
  private String createTripPlanSummary(TripPlanDetailsResponse tripPlan) {
    StringBuilder summary = new StringBuilder();

    // 여행 기본 정보
    summary.append("### 기본 정보\n");
    summary.append(String.format("- **제목**: %s\n", tripPlan.getTitle()));
    summary.append(
        String.format("- **기간**: %s ~ %s\n", tripPlan.getStartDate(), tripPlan.getEndDate()));

    // 일정 상세
    summary.append("### 일정 상세\n\n");

    for (DailyPlanResponse dailyPlan : tripPlan.getDailyPlans()) {
      summary.append(String.format("#### %d일차 (%s)\n", dailyPlan.dayNumber(), dailyPlan.date()));

      if (dailyPlan.scheduledPlaces().isEmpty()) {
        summary.append("- *(일정 없음)*\n\n");
        continue;
      }

      for (ScheduledPlaceResponse place : dailyPlan.scheduledPlaces()) {
        String timeRange =
            (place.startTime() != null && place.endTime() != null)
                ? String.format("%s ~ %s", place.startTime(), place.endTime())
                : "시간 미정";

        summary.append(
            String.format(
                "- **[%d]** %s (%s)\n", place.visitOrder(), place.place().name(), timeRange));
      }
      summary.append("\n");
    }

    return summary.toString();
  }
}
