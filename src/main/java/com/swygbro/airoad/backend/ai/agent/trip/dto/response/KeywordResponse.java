package com.swygbro.airoad.backend.ai.agent.trip.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.swygbro.airoad.backend.content.domain.entity.ProvinceType;

import lombok.Builder;

@Builder
public record KeywordResponse(
    @JsonPropertyDescription("대한민국 광역자치단체(시/도) 명칭") ProvinceType selectedProvince,
    @JsonPropertyDescription("'시/군/구' 단위의 기초자치단체 명칭. (주의: '동/읍/면' 단위는 너무 좁으므로 절대 사용 금지)")
        String selectedDistrict,
    @JsonPropertyDescription("장소 관련 키워드") List<String> places,
    @JsonPropertyDescription("음식점 관련 키워드") List<String> restaurants) {}
