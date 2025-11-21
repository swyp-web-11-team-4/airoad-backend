package com.swygbro.airoad.backend.ai.agent.trip.dto.response;

import java.util.List;

import org.springframework.ai.document.Document;

import lombok.Builder;

@Builder
public record PlaceSearchResponse(List<Document> places, List<Document> restaurants) {}
