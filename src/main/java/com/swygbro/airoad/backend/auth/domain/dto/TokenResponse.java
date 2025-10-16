package com.swygbro.airoad.backend.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResponse {
  private String accessToken;
  private Long accessTokenExpiresIn;
  private String refreshToken;
  private Long refreshTokenExpiresIn;
  private String tokenType;

  public static TokenResponse from(
      String accessToken, String refreshToken, Long accessTokenExp, Long refreshTokenExp) {
    return TokenResponse.builder()
        .accessToken(accessToken)
        .accessTokenExpiresIn(accessTokenExp)
        .refreshToken(refreshToken)
        .refreshTokenExpiresIn(refreshTokenExp)
        .tokenType("Bearer")
        .build();
  }
}
