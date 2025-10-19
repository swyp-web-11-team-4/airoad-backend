package com.swygbro.airoad.backend.auth.application;

public interface TokenUseCase {

  void createRefreshToken(String token, Long userId);

  void deleteRefreshTokenByMemberId(Long memberId);
}
