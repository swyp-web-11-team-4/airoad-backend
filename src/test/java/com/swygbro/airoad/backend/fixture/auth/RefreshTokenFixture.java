package com.swygbro.airoad.backend.fixture.auth;

import java.time.LocalDateTime;

import com.swygbro.airoad.backend.auth.domain.entity.RefreshToken;

public class RefreshTokenFixture {

  public static RefreshToken create() {
    return RefreshToken.builder()
        .email("test@example.com")
        .token("valid.refresh.token")
        .expiresAt(LocalDateTime.now().plusDays(7))
        .build();
  }

  public static RefreshToken createExpired() {
    return RefreshToken.builder()
        .email("test@example.com")
        .token("expired.refresh.token")
        .expiresAt(LocalDateTime.now().minusDays(1))
        .build();
  }

  public static RefreshToken createWithEmail(String email) {
    return RefreshToken.builder()
        .email(email)
        .token("valid.refresh.token")
        .expiresAt(LocalDateTime.now().plusDays(7))
        .build();
  }

  public static RefreshToken createWithToken(String token) {
    return RefreshToken.builder()
        .email("test@example.com")
        .token(token)
        .expiresAt(LocalDateTime.now().plusDays(7))
        .build();
  }

  public static RefreshToken.RefreshTokenBuilder builder() {
    return RefreshToken.builder()
        .email("test@example.com")
        .token("valid.refresh.token")
        .expiresAt(LocalDateTime.now().plusDays(7));
  }
}
