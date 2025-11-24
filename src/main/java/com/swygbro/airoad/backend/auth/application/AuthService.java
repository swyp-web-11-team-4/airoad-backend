package com.swygbro.airoad.backend.auth.application;

import org.springframework.stereotype.Service;

import com.swygbro.airoad.backend.auth.domain.dto.refreshToken.RedisRefreshToken;
import com.swygbro.airoad.backend.auth.domain.dto.response.TokenResponse;
import com.swygbro.airoad.backend.auth.exception.AuthErrorCode;
import com.swygbro.airoad.backend.auth.infrastructure.RefreshTokenStore;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.common.infrastructure.encryption.SHA256Hasher;
import com.swygbro.airoad.backend.common.infrastructure.encryption.StringEncryptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 인증 관련 비즈니스 로직을 처리하는 서비스 구현체 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

  private final JwtTokenProvider jwtTokenProvider;
  private final SHA256Hasher sha256Hasher;
  private final StringEncryptor stringEncryptor;
  private final RefreshTokenStore refreshTokenStore;

  /** JWT 토큰 생성 및 Refresh Token 저장 */
  public TokenResponse createTokens(String email) {
    log.info("[AUTH] 토큰 생성 시작 - 이메일: {}", email);

    String accessToken = jwtTokenProvider.createAccessToken(email);
    String refreshToken = jwtTokenProvider.createRefreshToken(email);

    // 암호화 및 해싱
    String encryptedEmail = stringEncryptor.convertToDatabaseColumn(email);
    String emailHash = sha256Hasher.hash(email);
    String encryptedRefreshToken = stringEncryptor.convertToDatabaseColumn(refreshToken);
    String tokenHash = sha256Hasher.hash(refreshToken);

    log.info("[AUTH] Redis에 RefreshToken 저장 시작 - emailHash: {}", emailHash);
    log.debug("[AUTH] [로컬 테스트용] AccessToken: {}", accessToken);
    log.debug("[AUTH] [로컬 테스트용] RefreshToken: {}", refreshToken);

    // Redis에 저장
    saveRefreshToken(encryptedEmail, emailHash, encryptedRefreshToken, tokenHash);

    log.info("[AUTH] 토큰 생성 완료 - 이메일: {}", email);

    return TokenResponse.of(
        accessToken, refreshToken, jwtTokenProvider.getAccessTokenValidityInSeconds());
  }

  /** Refresh Token을 사용하여 새로운 Access Token과 Refresh Token 발급 */
  public TokenResponse reissue(String refreshToken) {
    log.info("[AUTH] 토큰 재발급 시작");

    // JWT 형식 검증
    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
    }

    String tokenHash = sha256Hasher.hash(refreshToken);
    log.info("[AUTH] Redis에서 RefreshToken 조회 - tokenHash: {}", tokenHash);

    // Redis에서 Refresh Token 조회
    RedisRefreshToken storedToken =
        refreshTokenStore
            .findByTokenHash(tokenHash)
            .orElseThrow(() -> new BusinessException(AuthErrorCode.UNSUPPORTED_TOKEN));

    // 복호화 및 정보 추출
    String encryptedEmail = storedToken.getEmail();
    String emailHash = storedToken.getEmailHash();
    String email = stringEncryptor.convertToEntityAttribute(encryptedEmail);

    log.info("[AUTH] 새로운 토큰 생성 - 이메일: {}", email);

    // 새로운 토큰 생성
    String newAccessToken = jwtTokenProvider.createAccessToken(email);
    String newRefreshToken = jwtTokenProvider.createRefreshToken(email);

    // 암호화 및 해싱
    String encryptedNewEmail = stringEncryptor.convertToDatabaseColumn(email);
    String encryptedNewRefreshToken = stringEncryptor.convertToDatabaseColumn(newRefreshToken);
    String newRefreshTokenHash = sha256Hasher.hash(newRefreshToken);

    // 기존 토큰 삭제
    log.info("[AUTH] Redis에서 기존 RefreshToken 삭제 - tokenHash: {}", tokenHash);
    refreshTokenStore.deleteByTokenHash(tokenHash);

    // 새 토큰 저장
    log.info("[AUTH] Redis에 새로운 RefreshToken 저장 - emailHash: {}", emailHash);
    log.debug("[AUTH] [로컬 테스트용] 재발급된 AccessToken: {}", newAccessToken);
    log.debug("[AUTH] [로컬 테스트용] 재발급된 RefreshToken: {}", newRefreshToken);

    RedisRefreshToken newToken =
        RedisRefreshToken.builder()
            .email(encryptedNewEmail)
            .emailHash(emailHash)
            .token(encryptedNewRefreshToken)
            .tokenHash(newRefreshTokenHash)
            .build();

    refreshTokenStore.save(newToken);

    log.info("[AUTH] 토큰 재발급 완료 - 이메일: {}", email);

    return TokenResponse.of(
        newAccessToken, newRefreshToken, jwtTokenProvider.getAccessTokenValidityInSeconds());
  }

  /** 로그아웃 - Refresh Token 삭제 */
  public void logout(String accessToken) {
    log.info("[AUTH] 로그아웃 시작");

    if (!jwtTokenProvider.validateToken(accessToken)) {
      throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
    }

    String email = jwtTokenProvider.getEmailFromToken(accessToken);
    String emailHash = sha256Hasher.hash(email);

    log.info("[AUTH] Redis에서 RefreshToken 존재 여부 확인 - emailHash: {}", emailHash);

    if (!refreshTokenStore.existsByEmailHash(emailHash)) {
      throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
    }

    log.info("[AUTH] Redis에서 RefreshToken 삭제 - emailHash: {}", emailHash);
    refreshTokenStore.deleteByEmailHash(emailHash);

    log.info("[AUTH] 로그아웃 완료 - 이메일: {}", email);
  }

  /** Refresh Token을 Redis에 저장 */
  private void saveRefreshToken(String email, String emailHash, String token, String tokenHash) {
    // 기존 토큰이 있으면 삭제 (emailHash로 삭제하면 두 키 모두 삭제됨)
    refreshTokenStore
        .findByEmailHash(emailHash)
        .ifPresent(
            oldToken -> {
              log.info("[AUTH] 기존 RefreshToken 발견, 삭제 처리 - emailHash: {}", emailHash);
              refreshTokenStore.deleteByEmailHash(emailHash);
            });

    RedisRefreshToken redisRefreshToken =
        RedisRefreshToken.builder()
            .email(email)
            .emailHash(emailHash)
            .token(token)
            .tokenHash(tokenHash)
            .build();

    log.info("[AUTH] Redis에 RefreshToken 저장 실행 - emailHash: {}", emailHash);
    refreshTokenStore.save(redisRefreshToken);
    log.info("[AUTH] Redis에 RefreshToken 저장 성공 - emailHash: {}", emailHash);
  }
}
