package com.swygbro.airoad.backend.ai.application.context.trip;

import java.util.List;

import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.application.context.dto.ScheduledPlaceTagContext;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor.MetadataEntry;
import com.swygbro.airoad.backend.ai.common.context.AbstractContextProvider;
import com.swygbro.airoad.backend.trip.application.TripPlanQueryUseCase;
import com.swygbro.airoad.backend.trip.domain.dto.response.ScheduledPlaceResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanDetailsResponse;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ScheduledPlaceTagContextProvider
    extends AbstractContextProvider<ScheduledPlaceTagContext> {

  private final TripPlanQueryUseCase tripPlanQueryUseCase;

  public ScheduledPlaceTagContextProvider(TripPlanQueryUseCase tripPlanQueryUseCase) {
    super(ScheduledPlaceTagContext.class);
    this.tripPlanQueryUseCase = tripPlanQueryUseCase;
  }

  @Override
  protected List<MetadataEntry> doGetContext(ScheduledPlaceTagContext data) {
    if (data.scheduledPlaceIdList() == null || data.scheduledPlaceIdList().isEmpty()) {
      log.debug("선택된 태그 장소가 없습니다");
      return List.of();
    }

    log.debug("태그 컨텍스트 생성 - {} 개 장소", data.scheduledPlaceIdList().size());

    TripPlanDetailsResponse tripPlan =
        tripPlanQueryUseCase.findTripPlanDetailsById(data.tripPlanId(), data.username());

    List<ScheduledPlaceResponse> taggedPlaces =
        tripPlan.dailyPlans().stream()
            .flatMap(dp -> dp.scheduledPlaces().stream())
            .filter(sp -> data.scheduledPlaceIdList().contains(sp.id()))
            .toList();

    if (taggedPlaces.isEmpty()) {
      log.debug("선택한 태그 ID와 일치하는 장소가 없습니다");
      return List.of();
    }

    String taggedPlacesSummary = createTaggedPlacesSummary(taggedPlaces);

    return PromptMetadataAdvisor.userMetadata(
        """
            **모든 응답을 반드시 유저가 선택한 태그 위주로 응답**해주세요.
            단, 일정 수정을 적용하기 위해서는 반드시 툴을 사용해야 합니다.

            %s
            """
            .formatted(taggedPlacesSummary));
  }

  private String createTaggedPlacesSummary(List<ScheduledPlaceResponse> taggedPlaces) {
    StringBuilder summary = new StringBuilder();

    summary.append("### 선택된 태그 장소\n\n");
    summary.append("| 방문순서 | 장소명 |\n");
    summary.append("|--------|--------|\n");

    for (ScheduledPlaceResponse place : taggedPlaces) {
      summary.append(String.format("| %d | %s |\n", place.visitOrder(), place.place().name()));
    }

    return summary.toString();
  }

  @Override
  public int getOrder() {
    return 15;
  }
}
