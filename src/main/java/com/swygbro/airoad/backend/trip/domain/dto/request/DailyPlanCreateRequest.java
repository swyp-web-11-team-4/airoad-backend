package com.swygbro.airoad.backend.trip.domain.dto.request;

import java.time.LocalDate;
import java.util.List;

import org.springframework.ai.tool.annotation.ToolParam;

import lombok.Builder;

/**
 * 일차별 여행 일정 정보를 전달하는 DTO입니다.
 *
 * <p>하루 동안 방문할 장소들의 목록과 날짜 정보를 담습니다.
 *
 * @param dayNumber 일차 (1일차, 2일차, ...)
 * @param date 해당 날짜
 * @param title 일정 제목
 * @param description 일정 설명
 * @param places 방문할 장소 목록 (방문 순서대로 정렬)
 */
@Builder
public record DailyPlanCreateRequest(
    Integer dayNumber,
    LocalDate date,
    String title,
    String description,
    List<ScheduledPlaceCreateRequest> places) {}
