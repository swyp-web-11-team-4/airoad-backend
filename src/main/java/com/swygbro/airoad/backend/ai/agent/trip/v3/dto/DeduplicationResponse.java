package com.swygbro.airoad.backend.ai.agent.trip.v3.dto;

import java.util.List;

import org.springframework.ai.document.Document;

/**
 * DeduplicationStep의 출력 (중복 제거 후)
 *
 * <p>SRP: 중복이 제거된 검색 결과만 포함
 */
public record DeduplicationResponse(List<Document> places, List<Document> restaurants) {

  public static DeduplicationResponse of(List<Document> places, List<Document> restaurants) {
    return new DeduplicationResponse(places, restaurants);
  }
}
