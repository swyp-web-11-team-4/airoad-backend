package com.swygbro.airoad.backend.trip.application;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.trip.domain.dto.request.DailyPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;
import com.swygbro.airoad.backend.trip.domain.embeddable.TravelSegment;
import com.swygbro.airoad.backend.trip.domain.entity.DailyPlan;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledPlace;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.domain.event.DailyPlanSavedEvent;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DailyPlanCommandService implements DailyPlanCommandUseCase {

  private final TripPlanRepository tripPlanRepository;
  private final PlaceRepository placeRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Tool(description = "유저의 일일 일정 계획을 저장합니다")
  public void saveDailyPlan(
      @ToolParam Long chatRoomId,
      @ToolParam Long tripPlanId,
      @ToolParam String username,
      @ToolParam DailyPlanCreateRequest request) {
    TripPlan tripPlan =
        tripPlanRepository
            .findById(tripPlanId)
            .orElseThrow(() -> new BusinessException(TripErrorCode.TRIP_PLAN_NOT_FOUND));

    DailyPlan dailyPlan =
        DailyPlan.builder()
            .date(request.date())
            .dayNumber(request.dayNumber())
            .title(request.title())
            .description(request.description())
            .build();

    List<Long> placeIds =
        request.places().stream().map(ScheduledPlaceCreateRequest::placeId).toList();

    Map<Long, Place> placeMap =
        placeIds.isEmpty()
            ? Map.of()
            : placeRepository.findAllByIdsWithThemes(placeIds).stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));

    request
        .places()
        .forEach(
            scheduledPlaceCreateRequest -> {
              Place place = placeMap.get(scheduledPlaceCreateRequest.placeId());
              if (place == null) {
                throw new BusinessException(TripErrorCode.PLACE_NOT_FOUND);
              }

              TravelSegment travelSegment =
                  TravelSegment.builder()
                      .travelTime(scheduledPlaceCreateRequest.travelTime())
                      .transportation(scheduledPlaceCreateRequest.transportation())
                      .build();

              ScheduledPlace scheduledPlace =
                  ScheduledPlace.builder()
                      .dailyPlan(dailyPlan)
                      .place(place)
                      .visitOrder(scheduledPlaceCreateRequest.visitOrder())
                      .category(scheduledPlaceCreateRequest.category())
                      .startTime(scheduledPlaceCreateRequest.startTime())
                      .endTime(scheduledPlaceCreateRequest.endTime())
                      .travelSegment(travelSegment)
                      .build();

              dailyPlan.addScheduledPlace(scheduledPlace);
            });

    dailyPlan.getScheduledPlaces().stream()
        .map(ScheduledPlace::getPlace)
        .findFirst()
        .map(Place::getImageUrl)
        .ifPresent(tripPlan::updateImageUrl);

    tripPlan.addDailyPlan(dailyPlan);

    TripPlan savedTripPlan = tripPlanRepository.save(tripPlan);

    DailyPlan savedDailyPlan =
        savedTripPlan.getDailyPlans().get(savedTripPlan.getDailyPlans().size() - 1);

    DailyPlanResponse response = DailyPlanResponse.of(savedDailyPlan);

    DailyPlanSavedEvent event =
        DailyPlanSavedEvent.builder()
            .chatRoomId(chatRoomId)
            .tripPlanId(tripPlanId)
            .username(username)
            .dailyPlan(response)
            .build();

    eventPublisher.publishEvent(event);

    log.info(
        "일일 여행 계획 저장 완료 - tripPlanId: {}, dayNumber: {}, isCompleted: {}",
        tripPlanId,
        request.dayNumber(),
        savedTripPlan.getIsCompleted());
  }
}
