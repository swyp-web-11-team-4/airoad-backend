package com.swygbro.airoad.backend.auth.application;

import com.swygbro.airoad.backend.auth.domain.dto.response.TokenResponse;
import com.swygbro.airoad.backend.auth.domain.entity.RefreshToken;
import com.swygbro.airoad.backend.auth.exception.AuthErrorCode;
import com.swygbro.airoad.backend.auth.infrastructure.RefreshTokenRepository;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 관련 비즈니스 로직을 처리하는 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;

  /**
   * JWT 토큰 생성 및 Refresh Token 저장
   */
  @Transactional
  public TokenResponse createTokens(String email) {
    String accessToken = jwtTokenProvider.createAccessToken(email);
    String refreshToken = jwtTokenProvider.createRefreshToken(email);

    // Refresh Token을 DB에 저장
    saveRefreshToken(email, refreshToken);

    log.info("Tokens created for user: {}", email);

    return TokenResponse.of(
        accessToken, refreshToken, jwtTokenProvider.getAccessTokenValidityInSeconds());
  }

  /**
   * Refresh Token을 사용하여 새로운 Access Token과 Refresh Token 발급
   */
  @Transactional
  public TokenResponse reissue(String refreshToken) {
    // JWT 형식 검증
    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
    }

    // DB에서 Refresh Token 조회
    RefreshToken storedToken =
        refreshTokenRepository
            .findByToken(refreshToken)
            .orElseThrow(() -> new BusinessException(AuthErrorCode.UNSUPPORTED_TOKEN));

    // 만료 여부 확인
    if (storedToken.isExpired()) {
      refreshTokenRepository.delete(storedToken);
      throw new BusinessException(AuthErrorCode.EXPIRED_TOKEN);
    }

    String email = storedToken.getEmail();
    String newAccessToken = jwtTokenProvider.createAccessToken(email);
    String newRefreshToken = jwtTokenProvider.createRefreshToken(email);

    // 새로운 Refresh Token으로 업데이트
    LocalDateTime expiresAt = jwtTokenProvider.getRefreshTokenExpiresAt();
    storedToken.updateToken(newRefreshToken, expiresAt);

    return TokenResponse.of(
        newAccessToken, newRefreshToken, jwtTokenProvider.getAccessTokenValidityInSeconds());
  }

  /**
   * 로그아웃 - Refresh Token 삭제
   */
  @Transactional
  public void logout(String accessToken) {
    if (!jwtTokenProvider.validateToken(accessToken)) {
      throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
    }

    String email = jwtTokenProvider.getEmailFromToken(accessToken);
    refreshTokenRepository.findByEmail(email).ifPresentOrElse(
        refreshTokenRepository::delete,
        () -> {
          throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    );
  }

  /**
   * Refresh Token을 DB에 저장 또는 업데이트
   */
  private void saveRefreshToken(String email, String token) {
    LocalDateTime expiresAt = jwtTokenProvider.getRefreshTokenExpiresAt();

    RefreshToken refreshToken =
        refreshTokenRepository
            .findByEmail(email)
            .map(
                existing -> {
                  existing.updateToken(token, expiresAt);
                  return existing;
                })
            .orElseGet(
                () ->
                    RefreshToken.builder().email(email).token(token).expiresAt(expiresAt).build());

    refreshTokenRepository.save(refreshToken);
  }
}
