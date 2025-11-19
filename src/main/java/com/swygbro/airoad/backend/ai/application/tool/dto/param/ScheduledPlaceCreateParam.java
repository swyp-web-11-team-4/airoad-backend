package com.swygbro.airoad.backend.ai.application.tool.dto.param;

import org.springframework.ai.tool.annotation.ToolParam;

import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;

import lombok.Builder;

@Builder
public record ScheduledPlaceCreateParam(
    @ToolParam(description = "장소 이름, 반드시 조회된 장소명과 정확히 일치하게 입력") String placeName,
    @ToolParam(description = "방문 순서") Integer visitOrder,
    @ToolParam(description = "일정 카테고리") ScheduledCategory category,
    @ToolParam(description = "대중교통 이용 예상 이동 시간 (분)") Integer travelTime) {}
