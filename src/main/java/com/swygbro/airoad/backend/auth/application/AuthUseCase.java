package com.swygbro.airoad.backend.auth.application;

import com.swygbro.airoad.backend.auth.domain.dto.response.TokenResponse;

public interface AuthUseCase {

  TokenResponse createTokens(String email);

  TokenResponse reissue(String refreshToken);

  void logout(String email);
}
