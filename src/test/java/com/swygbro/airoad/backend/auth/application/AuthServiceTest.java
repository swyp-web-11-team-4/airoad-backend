package com.swygbro.airoad.backend.auth.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.auth.domain.dto.TokenResponse;
import com.swygbro.airoad.backend.auth.domain.entity.TokenType;
import com.swygbro.airoad.backend.auth.filter.JwtTokenProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthServiceTest {

  @Mock private JwtTokenProvider jwtTokenProvider;

  @Mock private TokenService tokenService;

  @InjectMocks private AuthService authService;

  private String refreshToken;
  private String email;
  private String role;

  @BeforeEach
  void setUp() {
    refreshToken = "valid.refresh.token";
    email = "test@example.com";
    role = "MEMBER";
  }

  @Nested
  @DisplayName("reissue 메서드는")
  class Reissue {

    @Test
    @DisplayName("리프레시 토큰으로 새로운 액세스 토큰과 리프레시 토큰을 재발급한다")
    void shouldReissueNewTokens() {
      // given
      String newAccessToken = "new.access.token";
      String newRefreshToken = "new.refresh.token";

      given(
              jwtTokenProvider.getClaimFromToken(
                  eq(refreshToken), eq("email"), eq(String.class), eq(TokenType.REFRESH_TOKEN)))
          .willReturn(email);
      given(jwtTokenProvider.generateAccessToken(email)).willReturn(newAccessToken);
      given(jwtTokenProvider.generateRefreshToken(email)).willReturn(newRefreshToken);

      // when
      TokenResponse response = authService.reissue(refreshToken);

      // then
      assertThat(response).isNotNull();
      assertThat(response.accessToken()).isEqualTo(newAccessToken);
      assertThat(response.refreshToken()).isEqualTo(newRefreshToken);
      assertThat(response.tokenType()).isEqualTo("Bearer");

      verify(jwtTokenProvider)
          .getClaimFromToken(
              eq(refreshToken), eq("email"), eq(String.class), eq(TokenType.REFRESH_TOKEN));
      verify(tokenService).deleteRefreshTokenByEmail(email);
      verify(jwtTokenProvider).generateAccessToken(email);
      verify(jwtTokenProvider).generateRefreshToken(email);
      verify(tokenService).createRefreshToken(newRefreshToken, email);
    }

    @Test
    @DisplayName("기존 리프레시 토큰을 삭제하고 새로운 리프레시 토큰을 저장한다")
    void shouldDeleteOldRefreshTokenAndSaveNewOne() {
      // given
      String newAccessToken = "new.access.token";
      String newRefreshToken = "new.refresh.token";

      given(
              jwtTokenProvider.getClaimFromToken(
                  anyString(), eq("email"), eq(String.class), eq(TokenType.REFRESH_TOKEN)))
          .willReturn(email);
      given(jwtTokenProvider.generateAccessToken(anyString())).willReturn(newAccessToken);
      given(jwtTokenProvider.generateRefreshToken(anyString())).willReturn(newRefreshToken);

      // when
      authService.reissue(refreshToken);

      // then
      verify(tokenService).deleteRefreshTokenByEmail(email);
      verify(tokenService).createRefreshToken(newRefreshToken, email);
    }
  }

  @Nested
  @DisplayName("logout 메서드는")
  class Logout {

    @Test
    @DisplayName("리프레시 토큰에서 이메일을 추출하여 토큰을 삭제한다")
    void shouldDeleteRefreshTokenOnLogout() {
      // given
      given(
              jwtTokenProvider.getClaimFromToken(
                  eq(refreshToken), eq("email"), eq(String.class), eq(TokenType.REFRESH_TOKEN)))
          .willReturn(email);

      // when
      authService.logout(refreshToken);

      // then
      verify(jwtTokenProvider)
          .getClaimFromToken(
              eq(refreshToken), eq("email"), eq(String.class), eq(TokenType.REFRESH_TOKEN));
      verify(tokenService).deleteRefreshTokenByEmail(email);
    }

    @Test
    @DisplayName("로그아웃 시 리프레시 토큰만 삭제하고 액세스 토큰은 생성하지 않는다")
    void shouldOnlyDeleteRefreshTokenWithoutGeneratingNewTokens() {
      // given
      given(
              jwtTokenProvider.getClaimFromToken(
                  anyString(), eq("email"), eq(String.class), eq(TokenType.REFRESH_TOKEN)))
          .willReturn(email);

      // when
      authService.logout(refreshToken);

      // then
      verify(tokenService).deleteRefreshTokenByEmail(email);
      verify(jwtTokenProvider, Mockito.never()).generateAccessToken(anyString());
      verify(jwtTokenProvider, Mockito.never()).generateRefreshToken(anyString());
      verify(tokenService, Mockito.never()).createRefreshToken(anyString(), anyString());
    }
  }
}
