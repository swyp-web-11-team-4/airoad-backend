package com.swygbro.airoad.backend.auth.domain.dto.response;

import lombok.Builder;

@Builder
public record TokenResponse(String accessToken, String refreshToken, long accessTokenExpiresIn) {
  public static TokenResponse of(
      String accessToken, String refreshToken, long accessTokenExpiresIn) {
    return TokenResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .accessTokenExpiresIn(accessTokenExpiresIn)
        .build();
  }
}
