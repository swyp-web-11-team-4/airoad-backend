package com.swygbro.airoad.backend.auth.application;

public interface TokenUseCase {

  void createRefreshToken(String token, String email);

  void deleteRefreshTokenByEmail(String email);
}
