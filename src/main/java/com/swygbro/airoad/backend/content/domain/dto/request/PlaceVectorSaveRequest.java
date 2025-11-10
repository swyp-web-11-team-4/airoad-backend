package com.swygbro.airoad.backend.content.domain.dto.request;

import java.util.List;

import lombok.Builder;

@Builder
public record PlaceVectorSaveRequest(
    Long placeId, String name, String address, List<String> themes, String content) {}
