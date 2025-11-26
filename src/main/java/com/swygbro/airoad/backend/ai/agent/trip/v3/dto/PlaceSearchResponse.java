package com.swygbro.airoad.backend.ai.agent.trip.v3.dto;

import java.util.List;

import org.springframework.ai.document.Document;

/**
 * PlaceSearchStep의 출력 (중복 제거 전)
 *
 * <p>SRP: 순수 검색 결과만 포함
 */
public record PlaceSearchResponse(List<Document> places, List<Document> restaurants) {

  public static PlaceSearchResponse of(List<Document> places, List<Document> restaurants) {
    return new PlaceSearchResponse(places, restaurants);
  }
}
