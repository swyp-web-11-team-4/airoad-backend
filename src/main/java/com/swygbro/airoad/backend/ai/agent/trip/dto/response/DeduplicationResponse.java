package com.swygbro.airoad.backend.ai.agent.trip.dto.response;

import java.util.List;

import org.springframework.ai.document.Document;

public record DeduplicationResponse(List<Document> places, List<Document> restaurants) {

  public static DeduplicationResponse of(List<Document> places, List<Document> restaurants) {
    return new DeduplicationResponse(places, restaurants);
  }
}
