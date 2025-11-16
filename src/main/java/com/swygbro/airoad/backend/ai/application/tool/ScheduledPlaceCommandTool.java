package com.swygbro.airoad.backend.ai.application.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.application.tool.dto.ToolResponse;
import com.swygbro.airoad.backend.trip.application.ScheduledPlaceCommandUseCase;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.ScheduledPlaceUpdateRequest;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanUpdateStartedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledPlaceCommandTool {

  private final ScheduledPlaceCommandUseCase scheduledPlaceCommandUseCase;
  private final ApplicationEventPublisher eventPublisher;

  @Tool(description = """
      여행 일정에 새로운 장소를 추가할 때 사용합니다.
      추가된 일정은 자동으로 순서가 정렬됩니다.
      """)
  public ToolResponse addScheduledPlace(
      @ToolParam(description = "채팅방 고유 식별자") Long chatRoomId,
      @ToolParam(description = "여행 계획 고유 식별자") Long tripPlanId,
      @ToolParam(description = "사용자 이메일 주소") String username,
      @ToolParam(description = "장소를 추가할 일차 (1=첫째 날, 2=둘째 날, ...)") Integer dayNumber,
      @ToolParam(
              description =
                  """
          추가할 장소 정보를 담은 요청 객체:

          [필수] placeId:
            - 새로운 장소 추가 전 `searchPlace` 툴 호출 필요
            - 장소/음식점 컨텍스트에 존재하는 장소 ID 사용
            - 절대 임의 값 사용 금지

          [필수] category: 반드시 다음 3가지 중 하나만 사용:
            - MORNING: 오전 일정 (아침~오전, 아침식사, 오전 관광)
            - AFTERNOON: 오후 일정 (점심~오후, 점심식사, 오후 관광)
            - EVENING: 저녁 일정 (저녁~밤, 저녁식사, 야경, 밤 활동)
            [경고] LUNCH, DINNER, BREAKFAST 같은 값 절대 사용 금지

          [선택] visitOrder: 방문 순서 (일정의 마지막에 추가하는 경우에만 null 입력)
          [선택] travelTime: 이동 소요 예상 시간 분 단위
          [선택] transportation: 교통수단
          """)
          ScheduledPlaceCreateRequest request) {

    log.info(
        "[AI Tool] addScheduledPlace - username: {}, tripPlanId: {}, dayNumber: {}",
        username,
        tripPlanId,
        dayNumber);

    scheduledPlaceCommandUseCase.saveScheduledPlace(
        chatRoomId, tripPlanId, username, dayNumber, request);

    return ToolResponse.success(String.format("%d일차 여행 일정에 장소 추가 완료", dayNumber));
  }

  @Tool(description = """
      일정에 포함된 기존 장소의 속성(정보)을 수정할 때 사용합니다.
      """)
  public ToolResponse updateScheduledPlace(
      @ToolParam(description = "채팅방 고유 식별자") Long chatRoomId,
      @ToolParam(description = "여행 계획 고유 식별자") Long tripPlanId,
      @ToolParam(description = "사용자 이메일 주소") String username,
      @ToolParam(description = "수정할 장소가 속한 일차 (1=첫째 날, 2=둘째 날, ...)") Integer dayNumber,
      @ToolParam(description = "수정할 장소의 현재 방문 순서 (1=첫 번째, 2=두 번째, ...)") Integer visitOrder,
      @ToolParam(
              description =
                  """
      수정할 장소 정보를 담은 요청 객체:

      [선택] placeId: 장소를 다른 곳으로 교체하는 경우에만 사용
        - 장소/음식점 컨텍스트에 존재하는 장소 ID 사용
        - 절대 임의 값 사용 금지

      [선택] category: 시간대를 변경할 경우 반드시 다음 3가지 중 하나만 사용:
        - MORNING: 오전 일정 (아침~오전, 아침식사, 오전 관광)
        - AFTERNOON: 오후 일정 (점심~오후, 점심식사, 오후 관광)
        - EVENING: 저녁 일정 (저녁~밤, 저녁식사, 야경, 밤 활동)
        [경고] LUNCH, DINNER, BREAKFAST 같은 값 절대 사용 금지!

      [선택] travelTime: 이전 장소에서 이동 시간 (분 단위)
      [선택] transportation: 교통수단
      """)
          ScheduledPlaceUpdateRequest request) {

    log.info(
        "[AI Tool] updateScheduledPlace - username: {}, tripPlanId: {}, dayNumber: {}, visitOrder: {}",
        username,
        tripPlanId,
        dayNumber,
        visitOrder);

    publishScheduledPlaceEvent(
        TripPlanUpdateStartedEvent.builder()
            .chatRoomId(chatRoomId)
            .username(username)
            .message("%d일차 %d번째 장소 수정 요청을 수행합니다.".formatted(dayNumber, visitOrder))
            .tripPlanId(tripPlanId)
            .build());

    scheduledPlaceCommandUseCase.updateScheduledPlace(
        chatRoomId, tripPlanId, username, dayNumber, visitOrder, request);

    return ToolResponse.success(String.format("%d일차 %d번째 장소 수정 완료", dayNumber, visitOrder));
  }

  @Tool(description = """
      여행 일정에서 특정 장소를 삭제할 때 사용합니다.
      """)
  public ToolResponse deleteScheduledPlace(
      @ToolParam(description = "채팅방 고유 식별자") Long chatRoomId,
      @ToolParam(description = "여행 계획 고유 식별자") Long tripPlanId,
      @ToolParam(description = "사용자 이메일 주소") String username,
      @ToolParam(description = "삭제할 장소가 속한 일차 (1=첫째 날, 2=둘째 날, ...)") Integer dayNumber,
      @ToolParam(description = "삭제할 장소의 방문 순서 (1=첫 번째, 2=두 번째, ...)") Integer visitOrder) {

    log.info(
        "[AI Tool] deleteScheduledPlace - username: {}, tripPlanId: {}, dayNumber: {}, visitOrder: {}",
        username,
        tripPlanId,
        dayNumber,
        visitOrder);

    publishScheduledPlaceEvent(
        TripPlanUpdateStartedEvent.builder()
            .chatRoomId(chatRoomId)
            .username(username)
            .message("%d일차 %d번째 장소 삭제 요청을 수행합니다.".formatted(dayNumber, visitOrder))
            .tripPlanId(tripPlanId)
            .build());

    scheduledPlaceCommandUseCase.deleteScheduledPlace(
        chatRoomId, tripPlanId, username, dayNumber, visitOrder);

    return ToolResponse.success(String.format("%d일차 %d번째 장소 삭제 완료", dayNumber, visitOrder));
  }

  @Tool(description = """
      같은 날짜(일차) 내에서 두 장소의 방문 순서를 교환할 때 사용합니다.
      """)
  public ToolResponse swapScheduledPlaces(
      @ToolParam(description = "채팅방 고유 식별자") Long chatRoomId,
      @ToolParam(description = "여행 계획 고유 식별자") Long tripPlanId,
      @ToolParam(description = "사용자 이메일 주소") String username,
      @ToolParam(description = "장소들이 속한 일차 (1=첫째 날, 2=둘째 날, ...)") Integer dayNumber,
      @ToolParam(description = "교환할 첫 번째 장소의 방문 순서 (1=첫 번째, 2=두 번째, ...)") Integer visitOrderA,
      @ToolParam(description = "교환할 두 번째 장소의 방문 순서 (1=첫 번째, 2=두 번째, ...)") Integer visitOrderB) {

    log.info(
        "[AI Tool] swapScheduledPlaces - username: {}, tripPlanId: {}, dayNumber: {}, visitOrderA: {}, visitOrderB: {}",
        username,
        tripPlanId,
        dayNumber,
        visitOrderA,
        visitOrderB);

    publishScheduledPlaceEvent(
        TripPlanUpdateStartedEvent.builder()
            .chatRoomId(chatRoomId)
            .username(username)
            .message(
                "%d일차 %d번 <-> %d번 일정 순서 교체 요청을 수행합니다."
                    .formatted(dayNumber, visitOrderA, visitOrderB))
            .tripPlanId(tripPlanId)
            .build());

    scheduledPlaceCommandUseCase.swapScheduledPlaces(
        chatRoomId, tripPlanId, username, dayNumber, visitOrderA, visitOrderB);

    return ToolResponse.success(
        String.format("%d일차 %d번과 %d번 일정 순서 교체 완료", dayNumber, visitOrderA, visitOrderB));
  }

  private void publishScheduledPlaceEvent(Object event) {
    eventPublisher.publishEvent(event);
  }
}
