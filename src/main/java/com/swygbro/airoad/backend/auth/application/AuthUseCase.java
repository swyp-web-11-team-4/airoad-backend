package com.swygbro.airoad.backend.auth.application;

import com.swygbro.airoad.backend.auth.domain.dto.TokenResponse;

public interface AuthUseCase {
  TokenResponse reissueToken(String requestRefreshToken);

  void logout(String refreshToken);
}
