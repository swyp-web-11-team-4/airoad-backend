package com.swygbro.airoad.backend.trip.application;

import com.swygbro.airoad.backend.trip.domain.event.TripPlanUpdatedEvent;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
  public void saveDailyPlan(
      Long chatRoomId, Long tripPlanId, String username, DailyPlanCreateRequest request) {
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

              int visitOrder =
                  scheduledPlaceCreateRequest.visitOrder() != null
                      ? scheduledPlaceCreateRequest.visitOrder()
                      : dailyPlan.getScheduledPlaces().size() + 1;

              TravelSegment travelSegment =
                  TravelSegment.builder()
                      .travelTime(scheduledPlaceCreateRequest.travelTime())
                      .transportation(scheduledPlaceCreateRequest.transportation())
                      .build();

              ScheduledPlace scheduledPlace =
                  ScheduledPlace.builder()
                      .dailyPlan(dailyPlan)
                      .place(place)
                      .visitOrder(visitOrder)
                      .category(scheduledPlaceCreateRequest.category())
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

  @Override
  public void swapScheduledPlacesBetweenDays(
      Long chatRoomId,
      Long tripPlanId,
      String username,
      Integer dayNumberA,
      Integer visitOrderA,
      Integer dayNumberB,
      Integer visitOrderB) {
    log.info(
        "[시작] swapScheduledPlacesBetweenDays - 사용자: {}, 여행 계획 ID: {}, {}일차 {}번 <-> {}일차 {}번",
        username,
        tripPlanId,
        dayNumberA,
        visitOrderA,
        dayNumberB,
        visitOrderB);

    DailyPlan dailyPlanA = validateAndGetDailyPlan(tripPlanId, username, dayNumberA);
    DailyPlan dailyPlanB = validateAndGetDailyPlan(tripPlanId, username, dayNumberB);

    ScheduledPlace placeA = getScheduledPlace(dailyPlanA, visitOrderA);
    ScheduledPlace placeB = getScheduledPlace(dailyPlanB, visitOrderB);

    Place tempPlace = placeA.getPlace();
    placeA.updatePlace(placeB.getPlace());
    placeB.updatePlace(tempPlace);

    log.info(
        "[완료] swapScheduledPlacesBetweenDays - 여행 계획 ID: {}, {}일차 {}번 <-> {}일차 {}번 장소 교환 완료",
        tripPlanId,
        dayNumberA,
        visitOrderA,
        dayNumberB,
        visitOrderB);

    DailyPlanResponse dailyPlanResponseA = DailyPlanResponse.of(dailyPlanA);
    DailyPlanResponse dailyPlanResponseB = DailyPlanResponse.of(dailyPlanB);

    TripPlanUpdatedEvent eventA =
        TripPlanUpdatedEvent.builder()
            .chatRoomId(chatRoomId)
            .tripPlanId(tripPlanId)
            .username(username)
            .dailyPlan(dailyPlanResponseA)
            .build();

    TripPlanUpdatedEvent eventB =
        TripPlanUpdatedEvent.builder()
            .chatRoomId(chatRoomId)
            .tripPlanId(tripPlanId)
            .username(username)
            .dailyPlan(dailyPlanResponseB)
            .build();

    eventPublisher.publishEvent(eventA);
    eventPublisher.publishEvent(eventB);
  }

  private DailyPlan validateAndGetDailyPlan(Long tripPlanId, String username, Integer dayNumber) {
    TripPlan tripPlan =
        tripPlanRepository
            .findByIdWithDetails(tripPlanId)
            .orElseThrow(() -> new BusinessException(TripErrorCode.TRIP_PLAN_NOT_FOUND));

    if (!tripPlan.getMember().getEmail().equals(username)) {
      throw new BusinessException(TripErrorCode.TRIP_PLAN_FORBIDDEN);
    }

    return tripPlan.getDailyPlans().stream()
        .filter(dp -> dp.getDayNumber().equals(dayNumber))
        .findFirst()
        .orElseThrow(() -> new BusinessException(TripErrorCode.DAILY_PLAN_NOT_FOUND));
  }

  private ScheduledPlace getScheduledPlace(DailyPlan dailyPlan, Integer visitOrder) {
    return dailyPlan.getScheduledPlaces().stream()
        .filter(sp -> sp.getVisitOrder().equals(visitOrder))
        .findFirst()
        .orElseThrow(() -> new BusinessException(TripErrorCode.SCHEDULED_PLACE_NOT_FOUND));
  }
}
