package com.swygbro.airoad.backend.auth.application;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.auth.domain.dto.refreshToken.RedisRefreshToken;
import com.swygbro.airoad.backend.auth.domain.dto.response.TokenResponse;
import com.swygbro.airoad.backend.auth.exception.AuthErrorCode;
import com.swygbro.airoad.backend.auth.infrastructure.RefreshTokenStore;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.common.infrastructure.encryption.SHA256Hasher;
import com.swygbro.airoad.backend.common.infrastructure.encryption.StringEncryptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

  @Mock private JwtTokenProvider jwtTokenProvider;

  @Mock private SHA256Hasher sha256Hasher;

  @Mock private StringEncryptor stringEncryptor;

  @Mock private RefreshTokenStore refreshTokenStore;

  @InjectMocks private AuthService authService;

  private static final String TEST_EMAIL = "test@example.com";
  private static final String TEST_EMAIL_HASH = "hash_test@example.com";
  private static final String ENCRYPTED_TEST_EMAIL = "encrypted_test@example.com";
  private static final String TEST_ACCESS_TOKEN = "test.access.token";
  private static final String TEST_REFRESH_TOKEN = "test.refresh.token";
  private static final String TEST_REFRESH_TOKEN_HASH = "hash_test.refresh.token";
  private static final String ENCRYPTED_TEST_REFRESH_TOKEN = "encrypted_test.refresh.token";
  private static final String NEW_ACCESS_TOKEN = "new.access.token";
  private static final String NEW_REFRESH_TOKEN = "new.refresh.token";
  private static final String NEW_REFRESH_TOKEN_HASH = "hash_new.refresh.token";
  private static final String ENCRYPTED_NEW_REFRESH_TOKEN = "encrypted_new.refresh.token";
  private static final long TOKEN_VALIDITY_SECONDS = 3600L;

  @Nested
  @DisplayName("사용자가 로그인하면")
  class CreateTokens {

    @Test
    @DisplayName("액세스 토큰과 리프레시 토큰이 발급된다")
    void issuesBothTokens() {
      // given
      given(jwtTokenProvider.createAccessToken(TEST_EMAIL)).willReturn(TEST_ACCESS_TOKEN);
      given(jwtTokenProvider.createRefreshToken(TEST_EMAIL)).willReturn(TEST_REFRESH_TOKEN);
      given(jwtTokenProvider.getAccessTokenValidityInSeconds()).willReturn(TOKEN_VALIDITY_SECONDS);
      given(stringEncryptor.convertToDatabaseColumn(TEST_EMAIL)).willReturn(ENCRYPTED_TEST_EMAIL);
      given(stringEncryptor.convertToDatabaseColumn(TEST_REFRESH_TOKEN))
          .willReturn(ENCRYPTED_TEST_REFRESH_TOKEN);
      given(sha256Hasher.hash(TEST_EMAIL)).willReturn(TEST_EMAIL_HASH);
      given(sha256Hasher.hash(TEST_REFRESH_TOKEN)).willReturn(TEST_REFRESH_TOKEN_HASH);
      given(refreshTokenStore.findByEmailHash(TEST_EMAIL_HASH)).willReturn(Optional.empty());

      // when
      TokenResponse response = authService.createTokens(TEST_EMAIL);

      // then
      assertThat(response).isNotNull();
      assertThat(response.accessToken()).isEqualTo(TEST_ACCESS_TOKEN);
      assertThat(response.refreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
      assertThat(response.accessTokenExpiresIn()).isEqualTo(TOKEN_VALIDITY_SECONDS);
    }

    @Test
    @DisplayName("리프레시 토큰이 Redis에 저장된다")
    void savesRefreshTokenToRedis() {
      // given
      given(jwtTokenProvider.createAccessToken(TEST_EMAIL)).willReturn(TEST_ACCESS_TOKEN);
      given(jwtTokenProvider.createRefreshToken(TEST_EMAIL)).willReturn(TEST_REFRESH_TOKEN);
      given(jwtTokenProvider.getAccessTokenValidityInSeconds()).willReturn(TOKEN_VALIDITY_SECONDS);
      given(stringEncryptor.convertToDatabaseColumn(TEST_EMAIL)).willReturn(ENCRYPTED_TEST_EMAIL);
      given(stringEncryptor.convertToDatabaseColumn(TEST_REFRESH_TOKEN))
          .willReturn(ENCRYPTED_TEST_REFRESH_TOKEN);
      given(sha256Hasher.hash(TEST_EMAIL)).willReturn(TEST_EMAIL_HASH);
      given(sha256Hasher.hash(TEST_REFRESH_TOKEN)).willReturn(TEST_REFRESH_TOKEN_HASH);
      given(refreshTokenStore.findByEmailHash(TEST_EMAIL_HASH)).willReturn(Optional.empty());

      // when
      authService.createTokens(TEST_EMAIL);

      // then
      ArgumentCaptor<RedisRefreshToken> tokenCaptor =
          ArgumentCaptor.forClass(RedisRefreshToken.class);
      verify(refreshTokenStore).save(tokenCaptor.capture());

      RedisRefreshToken savedToken = tokenCaptor.getValue();
      assertThat(savedToken.getEmail()).isEqualTo(ENCRYPTED_TEST_EMAIL);
      assertThat(savedToken.getEmailHash()).isEqualTo(TEST_EMAIL_HASH);
      assertThat(savedToken.getToken()).isEqualTo(ENCRYPTED_TEST_REFRESH_TOKEN);
      assertThat(savedToken.getTokenHash()).isEqualTo(TEST_REFRESH_TOKEN_HASH);
    }

    @Test
    @DisplayName("이미 리프레시 토큰이 있으면 기존 토큰을 삭제하고 새 토큰을 저장한다")
    void deletesOldTokenAndSavesNewToken() {
      // given
      RedisRefreshToken existingToken =
          RedisRefreshToken.builder()
              .email("old_encrypted_email")
              .emailHash(TEST_EMAIL_HASH)
              .token("old_encrypted_token")
              .tokenHash("old_token_hash")
              .build();

      given(jwtTokenProvider.createAccessToken(TEST_EMAIL)).willReturn(TEST_ACCESS_TOKEN);
      given(jwtTokenProvider.createRefreshToken(TEST_EMAIL)).willReturn(TEST_REFRESH_TOKEN);
      given(jwtTokenProvider.getAccessTokenValidityInSeconds()).willReturn(TOKEN_VALIDITY_SECONDS);
      given(stringEncryptor.convertToDatabaseColumn(TEST_EMAIL)).willReturn(ENCRYPTED_TEST_EMAIL);
      given(stringEncryptor.convertToDatabaseColumn(TEST_REFRESH_TOKEN))
          .willReturn(ENCRYPTED_TEST_REFRESH_TOKEN);
      given(sha256Hasher.hash(TEST_EMAIL)).willReturn(TEST_EMAIL_HASH);
      given(sha256Hasher.hash(TEST_REFRESH_TOKEN)).willReturn(TEST_REFRESH_TOKEN_HASH);
      given(refreshTokenStore.findByEmailHash(TEST_EMAIL_HASH))
          .willReturn(Optional.of(existingToken));

      // when
      authService.createTokens(TEST_EMAIL);

      // then
      verify(refreshTokenStore).deleteByEmailHash(TEST_EMAIL_HASH);
      verify(refreshTokenStore).save(any(RedisRefreshToken.class));
    }
  }

  @Nested
  @DisplayName("사용자가 토큰 재발급을 요청하면")
  class Reissue {

    @Test
    @DisplayName("유효한 리프레시 토큰으로 새로운 토큰 쌍이 발급된다")
    void issuesNewTokensWithValidRefreshToken() {
      // given
      RedisRefreshToken storedToken =
          RedisRefreshToken.builder()
              .email(ENCRYPTED_TEST_EMAIL)
              .emailHash(TEST_EMAIL_HASH)
              .token(ENCRYPTED_TEST_REFRESH_TOKEN)
              .tokenHash(TEST_REFRESH_TOKEN_HASH)
              .build();

      given(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
      given(sha256Hasher.hash(TEST_REFRESH_TOKEN)).willReturn(TEST_REFRESH_TOKEN_HASH);
      given(refreshTokenStore.findByTokenHash(TEST_REFRESH_TOKEN_HASH))
          .willReturn(Optional.of(storedToken));
      given(stringEncryptor.convertToEntityAttribute(ENCRYPTED_TEST_EMAIL)).willReturn(TEST_EMAIL);
      given(jwtTokenProvider.createAccessToken(TEST_EMAIL)).willReturn(NEW_ACCESS_TOKEN);
      given(jwtTokenProvider.createRefreshToken(TEST_EMAIL)).willReturn(NEW_REFRESH_TOKEN);
      given(stringEncryptor.convertToDatabaseColumn(TEST_EMAIL)).willReturn(ENCRYPTED_TEST_EMAIL);
      given(stringEncryptor.convertToDatabaseColumn(NEW_REFRESH_TOKEN))
          .willReturn(ENCRYPTED_NEW_REFRESH_TOKEN);
      given(sha256Hasher.hash(NEW_REFRESH_TOKEN)).willReturn(NEW_REFRESH_TOKEN_HASH);
      given(jwtTokenProvider.getAccessTokenValidityInSeconds()).willReturn(TOKEN_VALIDITY_SECONDS);

      // when
      TokenResponse response = authService.reissue(TEST_REFRESH_TOKEN);

      // then
      assertThat(response).isNotNull();
      assertThat(response.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
      assertThat(response.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
      assertThat(response.accessTokenExpiresIn()).isEqualTo(TOKEN_VALIDITY_SECONDS);
    }

    @Test
    @DisplayName("기존 토큰은 삭제되고 새 토큰이 저장된다")
    void deletesOldTokenAndSavesNewToken() {
      // given
      RedisRefreshToken storedToken =
          RedisRefreshToken.builder()
              .email(ENCRYPTED_TEST_EMAIL)
              .emailHash(TEST_EMAIL_HASH)
              .token(ENCRYPTED_TEST_REFRESH_TOKEN)
              .tokenHash(TEST_REFRESH_TOKEN_HASH)
              .build();

      given(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
      given(sha256Hasher.hash(TEST_REFRESH_TOKEN)).willReturn(TEST_REFRESH_TOKEN_HASH);
      given(refreshTokenStore.findByTokenHash(TEST_REFRESH_TOKEN_HASH))
          .willReturn(Optional.of(storedToken));
      given(stringEncryptor.convertToEntityAttribute(ENCRYPTED_TEST_EMAIL)).willReturn(TEST_EMAIL);
      given(jwtTokenProvider.createAccessToken(TEST_EMAIL)).willReturn(NEW_ACCESS_TOKEN);
      given(jwtTokenProvider.createRefreshToken(TEST_EMAIL)).willReturn(NEW_REFRESH_TOKEN);
      given(stringEncryptor.convertToDatabaseColumn(TEST_EMAIL)).willReturn(ENCRYPTED_TEST_EMAIL);
      given(stringEncryptor.convertToDatabaseColumn(NEW_REFRESH_TOKEN))
          .willReturn(ENCRYPTED_NEW_REFRESH_TOKEN);
      given(sha256Hasher.hash(NEW_REFRESH_TOKEN)).willReturn(NEW_REFRESH_TOKEN_HASH);
      given(jwtTokenProvider.getAccessTokenValidityInSeconds()).willReturn(TOKEN_VALIDITY_SECONDS);

      // when
      authService.reissue(TEST_REFRESH_TOKEN);

      // then
      verify(refreshTokenStore).deleteByTokenHash(TEST_REFRESH_TOKEN_HASH);
      verify(refreshTokenStore).save(any(RedisRefreshToken.class));
    }

    @Test
    @DisplayName("잘못된 형식의 토큰으로 요청하면 실패한다")
    void failsWithInvalidTokenFormat() {
      // given
      String invalidToken = "invalid.token.format";
      given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

      // when & then
      assertThatThrownBy(() -> authService.reissue(invalidToken))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("등록되지 않은 토큰으로 요청하면 실패한다")
    void failsWithUnregisteredToken() {
      // given
      given(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
      given(sha256Hasher.hash(TEST_REFRESH_TOKEN)).willReturn(TEST_REFRESH_TOKEN_HASH);
      given(refreshTokenStore.findByTokenHash(TEST_REFRESH_TOKEN_HASH))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> authService.reissue(TEST_REFRESH_TOKEN))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.UNSUPPORTED_TOKEN);
    }
  }

  @Nested
  @DisplayName("인증된 사용자가 로그아웃하면")
  class Logout {

    @Test
    @DisplayName("저장된 리프레시 토큰이 Redis에서 삭제된다")
    void deletesRefreshTokenFromRedis() {
      // given
      given(jwtTokenProvider.validateToken(TEST_ACCESS_TOKEN)).willReturn(true);
      given(jwtTokenProvider.getEmailFromToken(TEST_ACCESS_TOKEN)).willReturn(TEST_EMAIL);
      given(sha256Hasher.hash(TEST_EMAIL)).willReturn(TEST_EMAIL_HASH);
      given(refreshTokenStore.existsByEmailHash(TEST_EMAIL_HASH)).willReturn(true);

      // when
      authService.logout(TEST_ACCESS_TOKEN);

      // then
      verify(refreshTokenStore).deleteByEmailHash(TEST_EMAIL_HASH);
    }
  }
}
