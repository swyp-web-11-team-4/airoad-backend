package com.swygbro.airoad.backend.auth.application;

import java.time.LocalDateTime;
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

import com.swygbro.airoad.backend.auth.domain.dto.response.TokenResponse;
import com.swygbro.airoad.backend.auth.domain.entity.RefreshToken;
import com.swygbro.airoad.backend.auth.exception.AuthErrorCode;
import com.swygbro.airoad.backend.auth.infrastructure.RefreshTokenRepository;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.fixture.auth.RefreshTokenFixture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

  @Mock private JwtTokenProvider jwtTokenProvider;

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks private AuthService authService;

  private static final String TEST_EMAIL = "test@example.com";
  private static final String TEST_ACCESS_TOKEN = "test.access.token";
  private static final String TEST_REFRESH_TOKEN = "test.refresh.token";
  private static final String NEW_ACCESS_TOKEN = "new.access.token";
  private static final String NEW_REFRESH_TOKEN = "new.refresh.token";
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
      given(jwtTokenProvider.getRefreshTokenExpiresAt())
          .willReturn(LocalDateTime.now().plusDays(7));
      given(refreshTokenRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());
      given(refreshTokenRepository.save(any(RefreshToken.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      TokenResponse response = authService.createTokens(TEST_EMAIL);

      // then
      assertThat(response).isNotNull();
      assertThat(response.accessToken()).isEqualTo(TEST_ACCESS_TOKEN);
      assertThat(response.refreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
      assertThat(response.accessTokenExpiresIn()).isEqualTo(TOKEN_VALIDITY_SECONDS);
    }

    @Test
    @DisplayName("리프레시 토큰이 DB에 저장된다")
    void savesRefreshTokenToDatabase() {
      // given
      given(jwtTokenProvider.createAccessToken(TEST_EMAIL)).willReturn(TEST_ACCESS_TOKEN);
      given(jwtTokenProvider.createRefreshToken(TEST_EMAIL)).willReturn(TEST_REFRESH_TOKEN);
      given(jwtTokenProvider.getAccessTokenValidityInSeconds()).willReturn(TOKEN_VALIDITY_SECONDS);
      given(jwtTokenProvider.getRefreshTokenExpiresAt())
          .willReturn(LocalDateTime.now().plusDays(7));
      given(refreshTokenRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());
      given(refreshTokenRepository.save(any(RefreshToken.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      authService.createTokens(TEST_EMAIL);

      // then
      ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
      verify(refreshTokenRepository).save(tokenCaptor.capture());

      RefreshToken savedToken = tokenCaptor.getValue();
      assertThat(savedToken.getEmail()).isEqualTo(TEST_EMAIL);
      assertThat(savedToken.getToken()).isEqualTo(TEST_REFRESH_TOKEN);
      assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("이미 리프레시 토큰이 있으면 새로운 토큰으로 업데이트된다")
    void updatesExistingRefreshToken() {
      // given
      RefreshToken existingToken = RefreshTokenFixture.createWithEmail(TEST_EMAIL);
      given(jwtTokenProvider.createAccessToken(TEST_EMAIL)).willReturn(TEST_ACCESS_TOKEN);
      given(jwtTokenProvider.createRefreshToken(TEST_EMAIL)).willReturn(TEST_REFRESH_TOKEN);
      given(jwtTokenProvider.getAccessTokenValidityInSeconds()).willReturn(TOKEN_VALIDITY_SECONDS);
      given(jwtTokenProvider.getRefreshTokenExpiresAt())
          .willReturn(LocalDateTime.now().plusDays(7));
      given(refreshTokenRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(existingToken));
      given(refreshTokenRepository.save(any(RefreshToken.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      authService.createTokens(TEST_EMAIL);

      // then
      verify(refreshTokenRepository).save(existingToken);
      assertThat(existingToken.getToken()).isEqualTo(TEST_REFRESH_TOKEN);
    }
  }

  @Nested
  @DisplayName("사용자가 토큰 재발급을 요청하면")
  class Reissue {

    @Test
    @DisplayName("유효한 리프레시 토큰으로 새로운 토큰 쌍이 발급된다")
    void issuesNewTokensWithValidRefreshToken() {
      // given
      RefreshToken storedToken = RefreshTokenFixture.createWithEmail(TEST_EMAIL);
      given(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
      given(refreshTokenRepository.findByToken(TEST_REFRESH_TOKEN))
          .willReturn(Optional.of(storedToken));
      given(jwtTokenProvider.createAccessToken(TEST_EMAIL)).willReturn(NEW_ACCESS_TOKEN);
      given(jwtTokenProvider.createRefreshToken(TEST_EMAIL)).willReturn(NEW_REFRESH_TOKEN);
      given(jwtTokenProvider.getAccessTokenValidityInSeconds()).willReturn(TOKEN_VALIDITY_SECONDS);
      given(jwtTokenProvider.getRefreshTokenExpiresAt())
          .willReturn(LocalDateTime.now().plusDays(7));

      // when
      TokenResponse response = authService.reissue(TEST_REFRESH_TOKEN);

      // then
      assertThat(response).isNotNull();
      assertThat(response.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
      assertThat(response.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
      assertThat(response.accessTokenExpiresIn()).isEqualTo(TOKEN_VALIDITY_SECONDS);
    }

    @Test
    @DisplayName("리프레시 토큰이 새 값으로 업데이트된다")
    void updatesRefreshToken() {
      // given
      RefreshToken storedToken = RefreshTokenFixture.createWithEmail(TEST_EMAIL);
      String oldToken = storedToken.getToken();

      given(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
      given(refreshTokenRepository.findByToken(TEST_REFRESH_TOKEN))
          .willReturn(Optional.of(storedToken));
      given(jwtTokenProvider.createAccessToken(TEST_EMAIL)).willReturn(NEW_ACCESS_TOKEN);
      given(jwtTokenProvider.createRefreshToken(TEST_EMAIL)).willReturn(NEW_REFRESH_TOKEN);
      given(jwtTokenProvider.getAccessTokenValidityInSeconds()).willReturn(TOKEN_VALIDITY_SECONDS);
      given(jwtTokenProvider.getRefreshTokenExpiresAt())
          .willReturn(LocalDateTime.now().plusDays(7));

      // when
      authService.reissue(TEST_REFRESH_TOKEN);

      // then
      assertThat(storedToken.getToken()).isNotEqualTo(oldToken);
      assertThat(storedToken.getToken()).isEqualTo(NEW_REFRESH_TOKEN);
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
      given(refreshTokenRepository.findByToken(TEST_REFRESH_TOKEN)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> authService.reissue(TEST_REFRESH_TOKEN))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.UNSUPPORTED_TOKEN);
    }

    @Test
    @DisplayName("만료된 토큰으로 요청하면 실패한다")
    void failsWithExpiredToken() {
      // given
      RefreshToken expiredToken = RefreshTokenFixture.createExpired();
      given(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
      given(refreshTokenRepository.findByToken(TEST_REFRESH_TOKEN))
          .willReturn(Optional.of(expiredToken));

      // when & then
      assertThatThrownBy(() -> authService.reissue(TEST_REFRESH_TOKEN))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.EXPIRED_TOKEN);
    }

    @Test
    @DisplayName("만료된 토큰은 DB에서 삭제된다")
    void deletesExpiredToken() {
      // given
      RefreshToken expiredToken = RefreshTokenFixture.createExpired();
      given(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
      given(refreshTokenRepository.findByToken(TEST_REFRESH_TOKEN))
          .willReturn(Optional.of(expiredToken));

      // when
      try {
        authService.reissue(TEST_REFRESH_TOKEN);
      } catch (BusinessException e) {
        // 예외 발생 확인
      }

      // then
      verify(refreshTokenRepository).delete(expiredToken);
    }
  }

  @Nested
  @DisplayName("인증된 사용자가 로그아웃하면")
  class Logout {

    @Test
    @DisplayName("저장된 리프레시 토큰이 삭제된다")
    void deletesRefreshToken() {
      // given
      RefreshToken refreshToken = RefreshTokenFixture.createWithEmail(TEST_EMAIL);

      given(jwtTokenProvider.validateToken(TEST_ACCESS_TOKEN)).willReturn(true);
      given(jwtTokenProvider.getEmailFromToken(TEST_ACCESS_TOKEN)).willReturn(TEST_EMAIL);
      given(refreshTokenRepository.findByEmail(TEST_EMAIL))
          .willReturn(Optional.ofNullable(refreshToken));

      // when
      authService.logout(TEST_ACCESS_TOKEN);

      // then
      verify(refreshTokenRepository).delete(refreshToken);
    }
  }
}
