package com.swygbro.airoad.backend.content.domain.event;

import java.util.List;

import lombok.Builder;

/**
 * Place 요약 요청 이벤트
 *
 * <p>PlaceEmbeddingService에서 발행하여 PlaceSummaryAgent가 AI로 자연어 요약을 생성하도록 요청합니다.
 *
 * @param placeId Place ID
 * @param name 장소명
 * @param address 주소 (지역 정보 추출용)
 * @param description 장소 설명
 * @param themes 테마 목록
 */
@Builder
public record PlaceSummaryRequestedEvent(
    Long placeId, String name, String address, String description, List<String> themes) {}
