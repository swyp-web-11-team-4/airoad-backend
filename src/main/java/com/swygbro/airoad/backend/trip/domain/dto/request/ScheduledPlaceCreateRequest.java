package com.swygbro.airoad.backend.trip.domain.dto.request;

import org.springframework.ai.tool.annotation.ToolParam;

import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;

import lombok.Builder;

/**
 * 방문 장소 정보를 전달하는 DTO입니다.
 *
 * <p>일정 생성 및 조회 시 방문할 장소의 정보를 담습니다.
 *
 * @param placeId 장소 ID (Place 엔티티의 ID)
 * @param visitOrder 방문 순서 (1부터 시작, null인 경우 마지막에 추가)
 * @param category 일정 분류
 * @param travelTime 이전 장소로부터의 이동 시간 (분)
 * @param transportation 이동 수단
 */
@Builder
public record ScheduledPlaceCreateRequest(
    @ToolParam Long placeId,
    @ToolParam Integer visitOrder,
    @ToolParam ScheduledCategory category,
    @ToolParam Integer travelTime,
    @ToolParam Transportation transportation) {}
