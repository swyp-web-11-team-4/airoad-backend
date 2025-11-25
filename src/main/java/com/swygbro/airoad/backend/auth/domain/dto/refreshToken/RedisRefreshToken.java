package com.swygbro.airoad.backend.auth.domain.dto.refreshToken;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RedisRefreshToken {
  private final String email;
  private final String emailHash;
  private final String token;
  private final String tokenHash;

  @JsonCreator
  public RedisRefreshToken(
      @JsonProperty("email") String email,
      @JsonProperty("emailHash") String emailHash,
      @JsonProperty("token") String token,
      @JsonProperty("tokenHash") String tokenHash) {
    this.email = email;
    this.emailHash = emailHash;
    this.token = token;
    this.tokenHash = tokenHash;
  }
}
