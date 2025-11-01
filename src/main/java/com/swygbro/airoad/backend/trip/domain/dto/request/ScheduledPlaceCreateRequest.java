package com.swygbro.airoad.backend.trip.domain.dto.request;

import java.time.LocalTime;

import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;

import lombok.Builder;

/**
 * 방문 장소 정보를 전달하는 DTO입니다.
 *
 * <p>일정 생성 및 조회 시 방문할 장소의 정보를 담습니다.
 *
 * @param placeId 장소 ID (Place 엔티티의 ID)
 * @param visitOrder 방문 순서
 * @param category 일정 분류 (MORNING, LUNCH, AFTERNOON, CAFE, DINNER, EVENING)
 * @param startTime 계획된 시작 시간
 * @param endTime 계획된 종료 시간
 * @param travelTime 이전 장소로부터의 이동 시간 (분)
 * @param transportation 이동 수단
 */
@Builder
public record ScheduledPlaceCreateRequest(
    Long placeId,
    Integer visitOrder,
    ScheduledCategory category,
    LocalTime startTime,
    LocalTime endTime,
    Integer travelTime,
    Transportation transportation) {}
