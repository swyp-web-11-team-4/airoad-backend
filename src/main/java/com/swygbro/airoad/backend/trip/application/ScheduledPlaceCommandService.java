package com.swygbro.airoad.backend.trip.application;

import java.util.Comparator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceUpdateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;
import com.swygbro.airoad.backend.trip.domain.embeddable.TravelSegment;
import com.swygbro.airoad.backend.trip.domain.entity.DailyPlan;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledPlace;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanUpdatedEvent;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;
import com.swygbro.airoad.backend.trip.infrastructure.ScheduledPlaceRepository;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScheduledPlaceCommandService implements ScheduledPlaceCommandUseCase {

  private final TripPlanRepository tripPlanRepository;
  private final PlaceRepository placeRepository;
  private final ScheduledPlaceRepository scheduledPlaceRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  public void saveScheduledPlace(
      Long chatRoomId,
      Long tripPlanId,
      String username,
      Integer dayNumber,
      ScheduledPlaceCreateRequest request) {
    log.info(
        "[시작] saveScheduledPlace - 사용자: {}, 여행 계획 ID: {}, 일차: {}", username, tripPlanId, dayNumber);
    log.info("요청 정보: {}", request.toString());

    DailyPlan dailyPlan = validateAndGetDailyPlan(tripPlanId, username, dayNumber);

    Place place =
        placeRepository
            .findById(request.placeId())
            .orElseThrow(() -> new BusinessException(TripErrorCode.PLACE_NOT_FOUND));

    int targetVisitOrder =
        request.visitOrder() != null
            ? request.visitOrder()
            : dailyPlan.getScheduledPlaces().size() + 1;

    dailyPlan.getScheduledPlaces().stream()
        .filter(sp -> sp.getVisitOrder() >= targetVisitOrder)
        .forEach(sp -> sp.updateVisitOrder(sp.getVisitOrder() + 1));

    TravelSegment travelSegment =
        TravelSegment.builder()
            .travelTime(request.travelTime())
            .transportation(request.transportation())
            .build();

    ScheduledPlace scheduledPlace =
        ScheduledPlace.builder()
            .place(place)
            .visitOrder(targetVisitOrder)
            .category(request.category())
            .travelSegment(travelSegment)
            .build();

    dailyPlan.addScheduledPlace(scheduledPlace);
    dailyPlan.getScheduledPlaces().sort(Comparator.comparing(ScheduledPlace::getVisitOrder));

    log.info(
        "[완료] saveScheduledPlace - 여행 계획 ID: {}, 일차: {}의 {}번 위치에 장소 추가 완료",
        tripPlanId,
        dayNumber,
        targetVisitOrder);

    publishEvent(
        TripPlanUpdatedEvent.builder()
            .chatRoomId(chatRoomId)
            .tripPlanId(tripPlanId)
            .username(username)
            .dailyPlan(DailyPlanResponse.of(dailyPlan))
            .build());
  }

  @Override
  public void updateScheduledPlace(
      Long chatRoomId,
      Long tripPlanId,
      String username,
      Integer dayNumber,
      Integer visitOrder,
      ScheduledPlaceUpdateRequest request) {
    log.info(
        "[시작] updateScheduledPlace - 사용자: {}, 여행 계획 ID: {}, 일차: {}, 방문 순서: {}",
        username,
        tripPlanId,
        dayNumber,
        visitOrder);

    DailyPlan dailyPlan = validateAndGetDailyPlan(tripPlanId, username, dayNumber);
    ScheduledPlace scheduledPlace = getScheduledPlace(dailyPlan, visitOrder);
    Place place = placeRepository.findById(request.placeId()).orElse(null);

    TravelSegment travelSegment =
        TravelSegment.builder()
            .travelTime(request.travelTime())
            .transportation(request.transportation())
            .build();

    scheduledPlace.update(place, visitOrder, request.category(), travelSegment);

    log.info(
        "[완료] updateScheduledPlace - 여행 계획 ID: {}, 일차: {}, 방문 순서: {} 장소 수정 완료",
        tripPlanId,
        dayNumber,
        visitOrder);

    publishEvent(
        TripPlanUpdatedEvent.builder()
            .chatRoomId(chatRoomId)
            .tripPlanId(tripPlanId)
            .username(username)
            .dailyPlan(DailyPlanResponse.of(dailyPlan))
            .build());
  }

  @Override
  public void deleteScheduledPlace(
      Long chatRoomId, Long tripPlanId, String username, Integer dayNumber, Integer visitOrder) {
    log.info(
        "[시작] deleteScheduledPlace - 사용자: {}, 여행 계획 ID: {}, 일차: {}, 방문 순서: {}",
        username,
        tripPlanId,
        dayNumber,
        visitOrder);

    DailyPlan dailyPlan = validateAndGetDailyPlan(tripPlanId, username, dayNumber);
    ScheduledPlace scheduledPlace = getScheduledPlace(dailyPlan, visitOrder);

    dailyPlan.removeScheduledPlace(scheduledPlace);

    // 삭제된 위치 이후의 모든 항목들의 visitOrder를 1씩 감소
    dailyPlan.getScheduledPlaces().stream()
        .filter(sp -> sp.getVisitOrder() > visitOrder)
        .forEach(sp -> sp.updateVisitOrder(sp.getVisitOrder() - 1));

    log.info(
        "[완료] deleteScheduledPlace - 여행 계획 ID: {}, 일차: {}, 방문 순서: {} 장소 삭제 완료",
        tripPlanId,
        dayNumber,
        visitOrder);

    publishEvent(
        TripPlanUpdatedEvent.builder()
            .chatRoomId(chatRoomId)
            .tripPlanId(tripPlanId)
            .username(username)
            .dailyPlan(DailyPlanResponse.of(dailyPlan))
            .build());
  }

  @Override
  public void swapScheduledPlaces(
      Long chatRoomId,
      Long tripPlanId,
      String username,
      Integer dayNumber,
      Integer visitOrderA,
      Integer visitOrderB) {
    log.info(
        "[시작] swapScheduledPlaces - 사용자: {}, 여행 계획 ID: {}, 일차: {}, 순서1: {}, 순서2: {}",
        username,
        tripPlanId,
        dayNumber,
        visitOrderA,
        visitOrderB);

    DailyPlan dailyPlan = validateAndGetDailyPlan(tripPlanId, username, dayNumber);

    ScheduledPlace placeA = getScheduledPlace(dailyPlan, visitOrderA);
    ScheduledPlace placeB = getScheduledPlace(dailyPlan, visitOrderB);

    Place tempPlace = placeA.getPlace();
    placeA.updatePlace(placeB.getPlace());
    placeB.updatePlace(tempPlace);

    log.info(
        "[완료] swapScheduledPlaces - 여행 계획 ID: {}, 일차: {}, {}번 <-> {}번 순서 교환 완료",
        tripPlanId,
        dayNumber,
        visitOrderA,
        visitOrderB);

    publishEvent(
        TripPlanUpdatedEvent.builder()
            .chatRoomId(chatRoomId)
            .tripPlanId(tripPlanId)
            .username(username)
            .dailyPlan(DailyPlanResponse.of(dailyPlan))
            .build());
  }

  @Override
  public boolean validateScheduledPlace(String username, Long scheduledPlaceId) {
    log.debug(
        "[검증] validateScheduledPlace - username: {}, scheduledPlaceId: {}",
        username,
        scheduledPlaceId);

    boolean isOwner = scheduledPlaceRepository.existsByIdAndOwner(scheduledPlaceId, username);

    if (!isOwner) {
      log.warn("[검증 실패] 사용자 {}는 scheduledPlaceId {}에 대한 권한이 없습니다", username, scheduledPlaceId);
      throw new BusinessException(TripErrorCode.SCHEDULED_PLACE_NOT_FOUND);
    }

    log.debug("[검증 성공] scheduledPlaceId {}는 사용자 {}의 소유입니다", scheduledPlaceId, username);
    return true;
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

  private void publishEvent(TripPlanUpdatedEvent event) {
    eventPublisher.publishEvent(event);
  }
}
