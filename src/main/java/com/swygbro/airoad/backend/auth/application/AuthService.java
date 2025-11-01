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
  public TokenResponse reissue(String refreshToken) {
    String email =
        jwtTokenProvider.getClaimFromToken(
            refreshToken, "email", String.class, TokenType.REFRESH_TOKEN);

    tokenService.deleteRefreshTokenByEmail(email);
    String newAccessToken = jwtTokenProvider.generateAccessToken(email);
    String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

    tokenService.createRefreshToken(newRefreshToken, email);

    return TokenResponse.from(newAccessToken, newRefreshToken);
  }

  @Override
  @Transactional
  public void logout(String refreshToken) {
    String email =
        jwtTokenProvider.getClaimFromToken(
            refreshToken, "email", String.class, TokenType.REFRESH_TOKEN);

    tokenService.deleteRefreshTokenByEmail(email);
  }
}
