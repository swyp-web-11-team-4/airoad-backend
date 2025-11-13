package com.swygbro.airoad.backend.trip.application;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceUpdateRequest;
import com.swygbro.airoad.backend.trip.domain.embeddable.TravelSegment;
import com.swygbro.airoad.backend.trip.domain.entity.DailyPlan;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledPlace;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;
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

  @Override
  @Tool(
      description =
          """
          여행 일정에 새로운 방문 일정을 추가합니다.
          사용자가 특정 장소를 여행 계획에 추가해달라고 명시적으로 요청했을 때 사용하세요.
          'placeId'는 단일 장소의 ID(Long)여야 하며, 배열 형태가 아닙니다.
          """)
  public void saveScheduledPlace(
      @ToolParam(description = "요청을 수행하는 사용자의 username(이메일)") String username,
      @ToolParam(description = "장소를 추가할 여행 계획 ID") Long tripPlanId,
      @ToolParam(description = "장소를 추가할 일차(예: 1은 1일차)") Integer dayNumber,
      @ToolParam(description = "추가할 장소의 상세 정보") ScheduledPlaceCreateRequest request) {
    log.info(
        "[시작] saveScheduledPlace - 사용자: {}, 여행 계획 ID: {}, 일차: {}", username, tripPlanId, dayNumber);
    log.info("요청 정보: {}", request.toString());

    DailyPlan dailyPlan = validateAndGetDailyPlan(tripPlanId, username, dayNumber);

    Place place =
        placeRepository
            .findById(request.placeId())
            .orElseThrow(() -> new BusinessException(TripErrorCode.PLACE_NOT_FOUND));

    TravelSegment travelSegment =
        TravelSegment.builder()
            .travelTime(request.travelTime())
            .transportation(request.transportation())
            .build();

    ScheduledPlace scheduledPlace =
        ScheduledPlace.builder()
            .place(place)
            .visitOrder(request.visitOrder())
            .category(request.category())
            .startTime(request.startTime())
            .endTime(request.endTime())
            .travelSegment(travelSegment)
            .build();

    dailyPlan.addScheduledPlace(scheduledPlace);
    log.info("[완료] saveScheduledPlace - 여행 계획 ID: {}, 일차: {}에 장소 추가 완료", tripPlanId, dayNumber);
  }

  @Override
  @Tool(
      description =
          """
          여행 계획에 이미 있는 장소의 방문 정보를 수정합니다.
          사용자가 'A 장소의 방문 시간을 오후 3시로 변경해줘'처럼 기존 일정의 수정을 요청할 때 사용하세요.
          """)
  public void updateScheduledPlace(
      @ToolParam(description = "요청을 수행하는 사용자의 username(이메일)") String username,
      @ToolParam(description = "수정할 장소가 포함된 여행 계획의 ID") Long tripPlanId,
      @ToolParam(description = "수정할 장소가 포함된 일차") Integer dayNumber,
      @ToolParam(description = "수정할 장소의 방문 순서") Integer visitOrder,
      @ToolParam(description = "수정될 장소의 상세 정보") ScheduledPlaceUpdateRequest request) {
    log.info(
        "[시작] updateScheduledPlace - 사용자: {}, 여행 계획 ID: {}, 일차: {}, 방문 순서: {}",
        username,
        tripPlanId,
        dayNumber,
        visitOrder);

    DailyPlan dailyPlan = validateAndGetDailyPlan(tripPlanId, username, dayNumber);
    ScheduledPlace scheduledPlace = getScheduledPlace(dailyPlan, visitOrder);

    TravelSegment travelSegment =
        TravelSegment.builder()
            .travelTime(request.travelTime())
            .transportation(request.transportation())
            .build();

    scheduledPlace.update(
        request.visitOrder(),
        request.category(),
        request.startTime(),
        request.endTime(),
        travelSegment);
    log.info(
        "[완료] updateScheduledPlace - 여행 계획 ID: {}, 일차: {}, 방문 순서: {} 장소 수정 완료",
        tripPlanId,
        dayNumber,
        visitOrder);
  }

  @Override
  @Tool(
      description =
          """
          여행 계획에서 특정 방문 장소를 삭제합니다.
          사용자가 'A 장소는 계획에서 빼줘'처럼 기존 일정에서 특정 장소의 삭제를 요청할 때 사용하세요.
          """)
  public void deleteScheduledPlace(
      @ToolParam(description = "요청을 수행하는 사용자의 username(이메일)") String username,
      @ToolParam(description = "삭제할 장소가 포함된 여행 계획의 ID") Long tripPlanId,
      @ToolParam(description = "삭제할 장소가 포함된 일차") Integer dayNumber,
      @ToolParam(description = "삭제할 장소의 방문 순서") Integer visitOrder) {
    log.info(
        "[시작] deleteScheduledPlace - 사용자: {}, 여행 계획 ID: {}, 일차: {}, 방문 순서: {}",
        username,
        tripPlanId,
        dayNumber,
        visitOrder);

    DailyPlan dailyPlan = validateAndGetDailyPlan(tripPlanId, username, dayNumber);
    ScheduledPlace scheduledPlace = getScheduledPlace(dailyPlan, visitOrder);

    dailyPlan.removeScheduledPlace(scheduledPlace);
    log.info(
        "[완료] deleteScheduledPlace - 여행 계획 ID: {}, 일차: {}, 방문 순서: {} 장소 삭제 완료",
        tripPlanId,
        dayNumber,
        visitOrder);
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
