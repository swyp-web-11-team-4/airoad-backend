package com.swygbro.airoad.backend.auth.application;

import com.swygbro.airoad.backend.auth.domain.dto.TokenResponse;

public interface AuthUseCase {
  TokenResponse reissue(String requestRefreshToken);

  void logout(String refreshToken);
}
