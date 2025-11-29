package com.swygbro.airoad.backend.ai.agent.trip.dto.response;

import java.util.List;

import org.springframework.ai.document.Document;

public record PlaceSearchResponse(List<Document> places, List<Document> restaurants) {

  public static PlaceSearchResponse of(List<Document> places, List<Document> restaurants) {
    return new PlaceSearchResponse(places, restaurants);
  }
}
