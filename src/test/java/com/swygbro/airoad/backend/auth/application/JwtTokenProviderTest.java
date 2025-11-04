package com.swygbro.airoad.backend.auth.application;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

  private JwtTokenProvider jwtTokenProvider;

  private static final String TEST_SECRET_KEY =
      "test-secret-key-for-jwt-token-generation-minimum-256-bits-required-for-hs256-algorithm";
  private static final long ACCESS_TOKEN_VALIDITY = 3600000L; // 1시간
  private static final long REFRESH_TOKEN_VALIDITY = 604800000L; // 7일
  private static final String TEST_EMAIL = "test@example.com";

  @BeforeEach
  void setUp() {
    jwtTokenProvider =
        new JwtTokenProvider(TEST_SECRET_KEY, ACCESS_TOKEN_VALIDITY, REFRESH_TOKEN_VALIDITY);
  }

  @Nested
  @DisplayName("사용자가 액세스 토큰을 발급받으면")
  class CreateAccessToken {

    @Test
    @DisplayName("유효한 JWT 토큰이 생성된다")
    void createValidJwtToken() {
      // given
      String email = TEST_EMAIL;

      // when
      String accessToken = jwtTokenProvider.createAccessToken(email);

      // then
      assertThat(accessToken).isNotNull();
      assertThat(accessToken.split("\\.")).hasSize(3); // JWT는 3개 파트로 구성 (header.payload.signature)
      assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();
      assertThat(jwtTokenProvider.getEmailFromToken(accessToken)).isEqualTo(email);
    }

    @Test
    @DisplayName("토큰에 설정된 유효기간이 포함된다")
    void tokenHasCorrectExpiration() {
      // given
      String email = TEST_EMAIL;

      // when
      String accessToken = jwtTokenProvider.createAccessToken(email);

      // then
      assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();
      assertThat(jwtTokenProvider.getAccessTokenValidityInSeconds())
          .isEqualTo(ACCESS_TOKEN_VALIDITY / 1000);
    }
  }

  @Nested
  @DisplayName("사용자가 리프레시 토큰을 발급받으면")
  class CreateRefreshToken {

    @Test
    @DisplayName("유효한 JWT 토큰이 생성된다")
    void createValidJwtToken() {
      // given
      String email = TEST_EMAIL;

      // when
      String refreshToken = jwtTokenProvider.createRefreshToken(email);

      // then
      assertThat(refreshToken).isNotNull();
      assertThat(refreshToken.split("\\.")).hasSize(3);
      assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
      assertThat(jwtTokenProvider.getEmailFromToken(refreshToken)).isEqualTo(email);
    }

    @Test
    @DisplayName("액세스 토큰과 다른 고유한 값을 가진다")
    void differentFromAccessToken() {
      // given
      String email = TEST_EMAIL;

      // when
      String accessToken = jwtTokenProvider.createAccessToken(email);
      String refreshToken = jwtTokenProvider.createRefreshToken(email);

      // then
      assertThat(refreshToken).isNotEqualTo(accessToken);
    }
  }

  @Nested
  @DisplayName("사용자가 토큰에서 이메일을 조회하면")
  class GetEmailFromToken {

    @Test
    @DisplayName("토큰 발급 시 사용한 원래 이메일이 반환된다")
    void returnsOriginalEmail() {
      // given
      String email = TEST_EMAIL;
      String token = jwtTokenProvider.createAccessToken(email);

      // when
      String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

      // then
      assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("액세스 토큰과 리프레시 토큰 모두에서 동일한 이메일을 확인할 수 있다")
    void extractSameEmailFromBothTokenTypes() {
      // given
      String email = TEST_EMAIL;
      String accessToken = jwtTokenProvider.createAccessToken(email);
      String refreshToken = jwtTokenProvider.createRefreshToken(email);

      // when
      String emailFromAccessToken = jwtTokenProvider.getEmailFromToken(accessToken);
      String emailFromRefreshToken = jwtTokenProvider.getEmailFromToken(refreshToken);

      // then
      assertThat(emailFromAccessToken).isEqualTo(email);
      assertThat(emailFromRefreshToken).isEqualTo(email);
    }
  }

  @Nested
  @DisplayName("사용자가 토큰의 유효성을 검증하면")
  class ValidateToken {

    @Test
    @DisplayName("정상적으로 발급된 토큰은 유효하다고 판단된다")
    void validTokenIsAccepted() {
      // given
      String token = jwtTokenProvider.createAccessToken(TEST_EMAIL);

      // when
      boolean isValid = jwtTokenProvider.validateToken(token);

      // then
      assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("다른 시스템에서 발급한 토큰은 거부된다")
    void tokenFromDifferentSystemIsRejected() {
      // given - 다른 secret key로 생성된 토큰
      String differentSecretKey =
          "different-secret-key-for-invalid-signature-test-minimum-256-bits-required-for-hs256";
      String invalidToken =
          Jwts.builder()
              .subject(TEST_EMAIL)
              .signWith(Keys.hmacShaKeyFor(differentSecretKey.getBytes()))
              .compact();

      // when
      boolean isValid = jwtTokenProvider.validateToken(invalidToken);

      // then
      assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("유효기간이 지난 토큰은 거부된다")
    void expiredTokenIsRejected() {
      // given - 즉시 만료되는 토큰 생성
      JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(TEST_SECRET_KEY, -1000L, -1000L);
      String expiredToken = expiredTokenProvider.createAccessToken(TEST_EMAIL);

      // when
      boolean isValid = jwtTokenProvider.validateToken(expiredToken);

      // then
      assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 거부된다")
    void malformedTokenIsRejected() {
      // given
      String malformedToken = "this.is.not.a.valid.jwt.token";

      // when
      boolean isValid = jwtTokenProvider.validateToken(malformedToken);

      // then
      assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("null 토큰은 거부된다")
    void nullTokenIsRejected() {
      // given
      String nullToken = null;

      // when
      boolean isValid = jwtTokenProvider.validateToken(nullToken);

      // then
      assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("빈 토큰은 거부된다")
    void emptyTokenIsRejected() {
      // given
      String emptyToken = "";

      // when
      boolean isValid = jwtTokenProvider.validateToken(emptyToken);

      // then
      assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("공백만 있는 토큰은 거부된다")
    void blankTokenIsRejected() {
      // given
      String blankToken = "   ";

      // when
      boolean isValid = jwtTokenProvider.validateToken(blankToken);

      // then
      assertThat(isValid).isFalse();
    }
  }

  @Nested
  @DisplayName("사용자가 액세스 토큰 유효 시간을 조회하면")
  class GetAccessTokenValidityInSeconds {

    @Test
    @DisplayName("초 단위로 변환된 유효 시간이 반환된다")
    void returnsValidityInSeconds() {
      // given - ACCESS_TOKEN_VALIDITY는 밀리초 단위

      // when
      long validityInSeconds = jwtTokenProvider.getAccessTokenValidityInSeconds();

      // then
      assertThat(validityInSeconds).isEqualTo(ACCESS_TOKEN_VALIDITY / 1000);
    }

    @Test
    @DisplayName("양수 값이 반환된다")
    void returnsPositiveValue() {
      // given

      // when
      long validityInSeconds = jwtTokenProvider.getAccessTokenValidityInSeconds();

      // then
      assertThat(validityInSeconds).isPositive();
    }
  }

  @Nested
  @DisplayName("사용자가 리프레시 토큰 만료 시간을 조회하면")
  class GetRefreshTokenExpiresAt {

    @Test
    @DisplayName("미래 시간이 LocalDateTime으로 반환된다")
    void returnsFutureLocalDateTime() {
      // given
      LocalDateTime beforeCall = LocalDateTime.now();

      // when
      LocalDateTime expiresAt = jwtTokenProvider.getRefreshTokenExpiresAt();

      // then
      assertThat(expiresAt).isNotNull();
      assertThat(expiresAt).isAfter(beforeCall);
    }

    @Test
    @DisplayName("현재보다 미래의 시간이 반환된다")
    void returnsTimeInFuture() {
      // given
      LocalDateTime now = LocalDateTime.now();

      // when
      LocalDateTime expiresAt = jwtTokenProvider.getRefreshTokenExpiresAt();

      // then
      assertThat(expiresAt).isAfter(now);
    }

    @Test
    @DisplayName("설정된 유효기간만큼 미래 시간이 계산된다")
    void calculatesCorrectFutureTime() {
      // given
      LocalDateTime now = LocalDateTime.now();
      long expectedSeconds = REFRESH_TOKEN_VALIDITY / 1000;

      // when
      LocalDateTime expiresAt = jwtTokenProvider.getRefreshTokenExpiresAt();

      // then
      // 약간의 오차를 허용 (초 단위 비교)
      LocalDateTime expectedTime = now.plusSeconds(expectedSeconds);
      assertThat(expiresAt).isBetween(expectedTime.minusSeconds(2), expectedTime.plusSeconds(2));
    }
  }
}
