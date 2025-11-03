package com.swygbro.airoad.backend.trip.application;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.trip.domain.dto.request.DailyPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.ScheduledPlaceResponse;
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
public class DailyPlanService implements DailyPlanUseCase {

  private final TripPlanRepository tripPlanRepository;
  private final PlaceRepository placeRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public void saveDailyPlan(
      Long chatRoomId, Long tripPlanId, String username, DailyPlanCreateRequest request) {
    TripPlan tripPlan =
        tripPlanRepository
            .findById(tripPlanId)
            .orElseThrow(() -> new BusinessException(TripErrorCode.TRIP_PLAN_NOT_FOUND));

    DailyPlan dailyPlan =
        DailyPlan.builder().date(request.date()).dayNumber(request.dayNumber()).build();

    request
        .places()
        .forEach(
            placeRequest -> {
              ScheduledPlace scheduledPlace = createScheduledPlace(dailyPlan, placeRequest);
              dailyPlan.addScheduledPlace(scheduledPlace);
            });

    tripPlan.addDailyPlan(dailyPlan);

    TripPlan savedTripPlan = tripPlanRepository.save(tripPlan);

    DailyPlan savedDailyPlan =
        savedTripPlan.getDailyPlans().get(savedTripPlan.getDailyPlans().size() - 1);

    DailyPlanResponse response = toDailyPlanResponse(savedDailyPlan, request.dayNumber());

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

  /**
   * ScheduledPlace 엔티티를 생성합니다.
   *
   * @param dailyPlan 소속될 일일 계획
   * @param request ScheduledPlace 생성 요청 DTO
   * @return 생성된 ScheduledPlace 엔티티
   */
  private ScheduledPlace createScheduledPlace(
      DailyPlan dailyPlan, ScheduledPlaceCreateRequest request) {

    Place place =
        placeRepository
            .findById(request.placeId())
            .orElseThrow(() -> new BusinessException(TripErrorCode.PLACE_NOT_FOUND));

    TravelSegment travelSegment =
        TravelSegment.builder()
            .travelTime(request.travelTime())
            .transportation(request.transportation())
            .build();

    return ScheduledPlace.builder()
        .dailyPlan(dailyPlan)
        .place(place)
        .visitOrder(request.visitOrder())
        .category(request.category())
        .startTime(request.startTime())
        .endTime(request.endTime())
        .travelSegment(travelSegment)
        .build();
  }

  /**
   * DailyPlan 엔티티를 Response DTO로 변환합니다.
   *
   * @param dailyPlan DailyPlan 엔티티
   * @param dayNumber 일차 번호
   * @return DailyPlanResponse
   */
  private DailyPlanResponse toDailyPlanResponse(DailyPlan dailyPlan, Integer dayNumber) {
    List<ScheduledPlaceResponse> scheduledPlaceResponses =
        dailyPlan.getScheduledPlaces().stream().map(this::toScheduledPlaceResponse).toList();

    return DailyPlanResponse.builder()
        .id(dailyPlan.getId())
        .dayNumber(dayNumber)
        .date(dailyPlan.getDate().toString())
        .title(dayNumber + "일차 여행")
        .description("AI가 생성한 " + dayNumber + "일차 여행 일정입니다.")
        .scheduledPlaces(scheduledPlaceResponses)
        .build();
  }

  /**
   * ScheduledPlace 엔티티를 Response DTO로 변환합니다.
   *
   * @param scheduledPlace ScheduledPlace 엔티티
   * @return ScheduledPlaceResponse
   */
  private ScheduledPlaceResponse toScheduledPlaceResponse(ScheduledPlace scheduledPlace) {
    return ScheduledPlaceResponse.builder()
        .id(scheduledPlace.getId())
        .placeId(scheduledPlace.getPlace().getId())
        .visitOrder(scheduledPlace.getVisitOrder())
        .category(scheduledPlace.getCategory().name())
        .startTime(scheduledPlace.getStartTime().toString())
        .endTime(scheduledPlace.getEndTime().toString())
        .travelTime(scheduledPlace.getTravelSegment().getTravelTime())
        .transportation(scheduledPlace.getTravelSegment().getTransportation().name())
        .build();
  }
}
