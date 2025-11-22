package com.swygbro.airoad.backend.trip.domain.dto.request;

import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;

import lombok.Builder;

/**
 * 방문 장소 정보 수정을 위한 DTO입니다.
 *
 * <p>수정 시 변경될 수 있는 정보들을 담습니다.
 */
@Builder
public record ScheduledPlaceUpdateRequest(
    Long placeId, ScheduledCategory category, Integer travelTime, Transportation transportation) {}
