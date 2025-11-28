package com.swygbro.airoad.backend.ai.application.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.application.tool.dto.common.ToolResponse;
import com.swygbro.airoad.backend.content.application.PlaceQueryUseCase;
import com.swygbro.airoad.backend.content.domain.dto.response.PlaceResponse;
import com.swygbro.airoad.backend.trip.application.ScheduledPlaceCommandUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledPlaceCommandTool {

  private final ScheduledPlaceCommandUseCase scheduledPlaceCommandUseCase;
  private final PlaceQueryUseCase placeQueryUseCase;

  @Tool(description = """
      일정에 포함된 기존 장소를 다른 장소로 교체할 때 사용합니다.
      """)
  public ToolResponse replaceScheduledPlace(
      @ToolParam(description = "채팅방 고유 식별자") Long chatRoomId,
      @ToolParam(description = "여행 계획 고유 식별자") Long tripPlanId,
      @ToolParam(description = "사용자 이메일 주소") String username,
      @ToolParam(description = "교체할 장소가 속한 일차 (1=첫째 날, 2=둘째 날, ...)") Integer dayNumber,
      @ToolParam(description = "교체할 장소의 현재 방문 순서 (1=첫 번째, 2=두 번째, ...)") Integer visitOrder,
      @ToolParam(description = "새로운 장소의 이름") String placeName) {

    log.info(
        "[AI Tool] replaceScheduledPlace - username: {}, tripPlanId: {}, dayNumber: {}, visitOrder: {}, placeName: {}",
        username,
        tripPlanId,
        dayNumber,
        visitOrder,
        placeName);

    PlaceResponse placeResponse = placeQueryUseCase.findPlaceByName(placeName);

    scheduledPlaceCommandUseCase.replaceScheduledPlace(
        chatRoomId, tripPlanId, username, dayNumber, visitOrder, placeResponse.id());

    return ToolResponse.success(
        String.format("%d일차 %d번째 장소를 '%s'로 교체 완료", dayNumber, visitOrder, placeName));
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

    scheduledPlaceCommandUseCase.swapScheduledPlaces(
        chatRoomId, tripPlanId, username, dayNumber, visitOrderA, visitOrderB);

    return ToolResponse.success(
        String.format("%d일차 %d번과 %d번 일정 순서 교체 완료", dayNumber, visitOrderA, visitOrderB));
  }
}
