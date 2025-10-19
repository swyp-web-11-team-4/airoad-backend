package com.swygbro.airoad.backend.auth.domain.dto;

import lombok.Builder;

@Builder
public record TokenResponse(String accessToken, String refreshToken, String tokenType) {
  public static TokenResponse from(String accessToken, String refreshToken) {
    return TokenResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .build();
  }
}
