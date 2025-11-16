package com.swygbro.airoad.backend.ai.application.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.application.tool.dto.ToolResponse;
import com.swygbro.airoad.backend.trip.application.DailyPlanCommandUseCase;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanUpdateStartedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyPlanCommandTool {

  private final DailyPlanCommandUseCase dailyPlanCommandUseCase;
  private final ApplicationEventPublisher eventPublisher;

  @Tool(description = """
      서로 다른 날짜(일차)에 있는 두 장소의 위치를 교환할 때 사용합니다.
      """)
  public ToolResponse swapScheduledPlacesBetweenDays(
      @ToolParam(description = "채팅방 고유 식별자") Long chatRoomId,
      @ToolParam(description = "여행 계획 고유 식별자") Long tripPlanId,
      @ToolParam(description = "사용자 이메일 주소") String username,
      @ToolParam(description = "첫 번째 장소가 속한 일차 (1=첫째 날, 2=둘째 날, ...)") Integer dayNumberA,
      @ToolParam(description = "첫 번째 장소의 방문 순서 (1=첫 번째, 2=두 번째, ...)") Integer visitOrderA,
      @ToolParam(description = "두 번째 장소가 속한 일차 (1=첫째 날, 2=둘째 날, ...)") Integer dayNumberB,
      @ToolParam(description = "두 번째 장소의 방문 순서 (1=첫 번째, 2=두 번째, ...)") Integer visitOrderB) {

    log.info(
        "[AI Tool] swapScheduledPlacesBetweenDays - username: {}, tripPlanId: {}, {}일차 {}번 <-> {}일차 {}번",
        username,
        tripPlanId,
        dayNumberA,
        visitOrderA,
        dayNumberB,
        visitOrderB);

    publishDailyPlanEvent(
        TripPlanUpdateStartedEvent.builder()
            .chatRoomId(chatRoomId)
            .username(username)
            .message(
                "%d일차 %d번 <-> %d일차 %d번 장소 교환 요청을 수행합니다."
                    .formatted(dayNumberA, visitOrderA, dayNumberB, visitOrderB))
            .tripPlanId(tripPlanId)
            .build());

    dailyPlanCommandUseCase.swapScheduledPlacesBetweenDays(
        chatRoomId, tripPlanId, username, dayNumberA, visitOrderA, dayNumberB, visitOrderB);

    return ToolResponse.success(
        String.format(
            "%d일차 %d번과 %d일차 %d번 장소 교환 완료", dayNumberA, visitOrderA, dayNumberB, visitOrderB));
  }

  private void publishDailyPlanEvent(Object event) {
    eventPublisher.publishEvent(event);
  }
}
