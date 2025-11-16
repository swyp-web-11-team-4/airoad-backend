package com.swygbro.airoad.backend.ai.application.context.trip;

import java.util.List;

import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.application.context.dto.TripPlanQueryContext;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor.MetadataEntry;
import com.swygbro.airoad.backend.ai.common.context.AbstractContextProvider;
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

    return PromptMetadataAdvisor.systemMetadata(
        """
        ## 여행 계획 컨텍스트 (Trip Plan Context)

        사용자의 현재 여행 계획 정보입니다.
        반드시 동일한 장소를 중복해서 일정에 포함하지 않도록 하세요.
        또한 동일한 여행 소제목을 중복해서 작성하지 않도록 하세요.

        %s

        """
            .formatted(summary));
  }

  @Override
  public int getOrder() {
    return 20;
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
    summary.append(String.format("- **제목**: %s\n", tripPlan.title()));
    summary.append(String.format("- **기간**: %s ~ %s\n", tripPlan.startDate(), tripPlan.endDate()));

    // 일정 상세
    if (tripPlan.dailyPlans().isEmpty()) {
      summary.append("아직 생성된 일정이 없습니다.");
    } else {
      createDailyPlansSummary(summary, tripPlan);
    }

    return summary.toString();
  }

  private void createDailyPlansSummary(StringBuilder summary, TripPlanDetailsResponse tripPlan) {
    summary.append("### 일정 상세\n\n");

    for (DailyPlanResponse dailyPlan : tripPlan.dailyPlans()) {
      summary.append(
          String.format(
              "#### %d일차: %s (%s)\n", dailyPlan.dayNumber(), dailyPlan.title(), dailyPlan.date()));

      if (dailyPlan.scheduledPlaces().isEmpty()) {
        summary.append("- *(일정 없음)*\n\n");
        continue;
      }

      for (ScheduledPlaceResponse place : dailyPlan.scheduledPlaces()) {
        summary.append(String.format("- **[%d]** %s\n", place.visitOrder(), place.place().name()));
      }

      summary.append("\n");
    }
  }
}
