package com.swygbro.airoad.backend.auth.application;

import com.swygbro.airoad.backend.auth.domain.dto.LoginResponse;
import com.swygbro.airoad.backend.auth.domain.dto.TokenResponse;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;

public interface AuthUseCase {
  LoginResponse socialLogin(ProviderType provider, String accessToken);

  TokenResponse reissueToken(String requestRefreshToken);

  void logout(String refreshToken);
}
