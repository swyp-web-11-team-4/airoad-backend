package com.swygbro.airoad.backend.auth.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.auth.domain.dto.TokenResponse;
import com.swygbro.airoad.backend.auth.domain.entity.TokenType;
import com.swygbro.airoad.backend.auth.filter.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {
  private final JwtTokenProvider jwtTokenProvider;
  private final TokenService tokenService;

  @Override
  @Transactional
  public TokenResponse reissueToken(String refreshToken) {
    Long userId =
        jwtTokenProvider.getClaimFromToken(
            refreshToken, "userId", Long.class, TokenType.REFRESH_TOKEN);
    String role =
        jwtTokenProvider.getClaimFromToken(
            refreshToken, "role", String.class, TokenType.REFRESH_TOKEN);

    tokenService.deleteRefreshTokenByMemberId(userId);
    String newAccessToken = jwtTokenProvider.generateAccessToken(userId, role);
    String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

    tokenService.createRefreshToken(newRefreshToken, userId);

    return TokenResponse.from(newAccessToken, newRefreshToken);
  }

  @Override
  @Transactional
  public void logout(String refreshToken) {
    Long memberId =
        jwtTokenProvider.getClaimFromToken(
            refreshToken, "userId", Long.class, TokenType.REFRESH_TOKEN);

    tokenService.deleteRefreshTokenByMemberId(memberId);
  }
}
