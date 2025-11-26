package com.swygbro.airoad.backend.ai.agent.trip.v3;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.AiDailyPlanResponse.AiScheduledPlace;
import com.swygbro.airoad.backend.ai.agent.trip.v3.pipeline.TripPlanPipeline;
import com.swygbro.airoad.backend.ai.common.agent.AiroadAgent;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.event.DailyPlanGeneratedEvent;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.trip.domain.dto.request.DailyPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceCreateRequest;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationCompletedEvent;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationErrorEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Primary
@Slf4j
@Component("tripAgentV3")
@RequiredArgsConstructor
public class TripAgent implements AiroadAgent {

  private final AgentType agentType = AgentType.TRIP_AGENT;
  private final ApplicationEventPublisher eventPublisher;
  private final TripPlanPipeline pipeline;

  @Override
  public boolean supports(AgentType agentType) {
    return this.agentType == agentType;
  }

  @Override
  public void execute(Object data) {
    AiDailyPlanRequest request = (AiDailyPlanRequest) data;

    log.info(
        "여행 일정 생성 시작 (v3 파이프라인) - 지역: {}, 기간: {}일, 여행 ID: {}",
        request.region(),
        request.duration(),
        request.tripPlanId());

    try {
      List<Long> allPreviousPlaceIds = new ArrayList<>();
      List<String> allVisitedDistricts = new ArrayList<>();

      for (int dayNumber = 1; dayNumber <= request.duration(); dayNumber++) {
        log.debug("{}일차 일정 생성 시작", dayNumber);

        var result = pipeline.execute(request, dayNumber, allPreviousPlaceIds, allVisitedDistricts);
        AiDailyPlanResponse dailyPlan = result.plan();

        DailyPlanCreateRequest dailyPlanCreateRequest = toDailyPlanDto(dailyPlan);
        DailyPlanGeneratedEvent event =
            DailyPlanGeneratedEvent.builder()
                .chatRoomId(request.chatRoomId())
                .tripPlanId(request.tripPlanId())
                .username(request.username())
                .dailyPlan(dailyPlanCreateRequest)
                .build();

        eventPublisher.publishEvent(event);

        List<Long> currentPlaceIds =
            dailyPlan.places().stream().map(AiScheduledPlace::placeId).toList();
        allPreviousPlaceIds.addAll(currentPlaceIds);

        String selectedDistrict = result.selectedDistrict();
        if (selectedDistrict != null) {
          allVisitedDistricts.add(selectedDistrict);
        }

        log.info(
            "{}일차 일정 생성 완료 (사용한 장소 {}개 누적, 방문 구역: {})",
            dayNumber,
            allPreviousPlaceIds.size(),
            selectedDistrict);
      }

      TripPlanGenerationCompletedEvent completedEvent =
          TripPlanGenerationCompletedEvent.builder()
              .chatRoomId(request.chatRoomId())
              .tripPlanId(request.tripPlanId())
              .username(request.username())
              .message("AI 여행 일정 생성 요청에 성공했습니다")
              .build();

      eventPublisher.publishEvent(completedEvent);
      log.info("여행 일정 전체 생성 완료 - 총 {}일", request.duration());

    } catch (Exception e) {
      log.error("AI 일정 생성 중 오류 발생", e);

      TripPlanGenerationErrorEvent errorEvent =
          TripPlanGenerationErrorEvent.builder()
              .chatRoomId(((AiDailyPlanRequest) data).chatRoomId())
              .tripPlanId(((AiDailyPlanRequest) data).tripPlanId())
              .username(((AiDailyPlanRequest) data).username())
              .errorCode(AiErrorCode.TRIP_PLAN_GENERATION_ERROR)
              .build();

      eventPublisher.publishEvent(errorEvent);
    }
  }

  private DailyPlanCreateRequest toDailyPlanDto(AiDailyPlanResponse aiDailyPlanResponse) {
    List<ScheduledPlaceCreateRequest> scheduledPlaces =
        aiDailyPlanResponse.places().stream()
            .map(
                p ->
                    ScheduledPlaceCreateRequest.builder()
                        .placeId(p.placeId())
                        .category(p.category())
                        .travelTime(p.travelTime())
                        .transportation(p.transportation())
                        .visitOrder(p.visitOrder())
                        .summary(p.summary())
                        .build())
            .toList();

    return DailyPlanCreateRequest.builder()
        .dayNumber(aiDailyPlanResponse.dayNumber())
        .date(aiDailyPlanResponse.date())
        .title(aiDailyPlanResponse.title())
        .description(aiDailyPlanResponse.description())
        .places(scheduledPlaces)
        .build();
  }
}
