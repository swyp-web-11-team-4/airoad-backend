package com.swygbro.airoad.backend.trip.domain.dto.request;

import java.time.LocalTime;

import org.springframework.ai.tool.annotation.ToolParam;

import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;

/**
 * 방문 장소 정보 수정을 위한 DTO입니다.
 *
 * <p>수정 시 변경될 수 있는 정보들을 담습니다.
 */
public record ScheduledPlaceUpdateRequest(
    @ToolParam Integer visitOrder,
    @ToolParam ScheduledCategory category,
    @ToolParam LocalTime startTime,
    @ToolParam LocalTime endTime,
    @ToolParam Integer travelTime,
    @ToolParam Transportation transportation) {}
