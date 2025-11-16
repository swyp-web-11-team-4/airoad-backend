package com.swygbro.airoad.backend.ai.application.context.dto;

import org.springframework.ai.vectorstore.SearchRequest;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Builder
public record PlaceVectorQueryContext(QueryType queryType, SearchRequest searchRequest) {

  @Getter
  @RequiredArgsConstructor
  public enum QueryType {
    PLACE("장소"),
    RESTAURANT("음식점");

    private final String description;
  }
}
