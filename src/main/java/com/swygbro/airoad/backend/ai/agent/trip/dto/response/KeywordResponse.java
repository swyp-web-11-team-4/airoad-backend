package com.swygbro.airoad.backend.ai.agent.trip.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.Builder;

@Builder
public record KeywordResponse(
    @JsonPropertyDescription("선택된 광역시/도 (예: 서울특별시, 부산광역시)") String selectedProvince,
    @JsonPropertyDescription("선택된 행정구역 (예: 강남구, 송파구)") String selectedDistrict,
    @JsonPropertyDescription("장소 관련 키워드") List<String> places,
    @JsonPropertyDescription("음식점 관련 키워드") List<String> restaurants) {}
