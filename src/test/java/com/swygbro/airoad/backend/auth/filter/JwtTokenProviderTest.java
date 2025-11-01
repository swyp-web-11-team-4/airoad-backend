package com.swygbro.airoad.backend.auth.filter;

import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.swygbro.airoad.backend.auth.domain.entity.TokenType;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class JwtTokenProviderTest {

  private JwtTokenProvider jwtTokenProvider;

  private String accessTokenSecret;
  private String refreshTokenSecret;
  private long accessTokenExpiration;
  private long refreshTokenExpiration;

  @BeforeEach
  void setUp() {
    jwtTokenProvider = new JwtTokenProvider();

    // Base64 URL 인코딩된 256비트 키 생성 (32바이트)
    byte[] accessKeyBytes = new byte[32];
    byte[] refreshKeyBytes = new byte[32];
    for (int i = 0; i < 32; i++) {
      accessKeyBytes[i] = (byte) (i + 1);
      refreshKeyBytes[i] = (byte) (i + 33);
    }

    accessTokenSecret = Base64.getUrlEncoder().encodeToString(accessKeyBytes);
    refreshTokenSecret = Base64.getUrlEncoder().encodeToString(refreshKeyBytes);
    accessTokenExpiration = 3600000L; // 1 hour
    refreshTokenExpiration = 604800000L; // 7 days

    ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenSecret", accessTokenSecret);
    ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenSecret", refreshTokenSecret);
    ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", accessTokenExpiration);
    ReflectionTestUtils.setField(
        jwtTokenProvider, "refreshTokenExpiration", refreshTokenExpiration);
  }

  @Nested
  @DisplayName("generateAccessToken 메서드는")
  class GenerateAccessToken {

    @Test
    @DisplayName("이메일을 포함한 액세스 토큰을 생성한다")
    void shouldGenerateAccessTokenWithEmail() {
      // given
      String email = "test@example.com";

      // when
      String token = jwtTokenProvider.generateAccessToken(email);

      // then
      assertThat(token).isNotNull();
      assertThat(token).isNotEmpty();

      // 토큰에서 클레임 추출하여 검증
      String extractedEmail =
          jwtTokenProvider.getClaimFromToken(token, "email", String.class, TokenType.ACCESS_TOKEN);

      assertThat(extractedEmail).isEqualTo(email);
    }
  }

  @Nested
  @DisplayName("generateRefreshToken 메서드는")
  class GenerateRefreshToken {

    @Test
    @DisplayName("이메일을 포함한 리프레시 토큰을 생성한다")
    void shouldGenerateRefreshTokenWithEmail() {
      // given
      String email = "test@example.com";

      // when
      String token = jwtTokenProvider.generateRefreshToken(email);

      // then
      assertThat(token).isNotNull();
      assertThat(token).isNotEmpty();

      // 토큰에서 클레임 추출하여 검증
      String extractedEmail =
          jwtTokenProvider.getClaimFromToken(token, "email", String.class, TokenType.REFRESH_TOKEN);
      String type =
          jwtTokenProvider.getClaimFromToken(token, "type", String.class, TokenType.REFRESH_TOKEN);

      assertThat(extractedEmail).isEqualTo(email);
      assertThat(type).isEqualTo("REFRESH_TOKEN");
    }
  }

  @Nested
  @DisplayName("getClaimFromToken 메서드는")
  class GetClaimFromToken {

    @Test
    @DisplayName("유효한 액세스 토큰에서 클레임을 추출한다")
    void shouldExtractClaimFromValidAccessToken() {
      // given
      String email = "test@example.com";
      String token = jwtTokenProvider.generateAccessToken(email);

      // when
      String extractedEmail =
          jwtTokenProvider.getClaimFromToken(token, "email", String.class, TokenType.ACCESS_TOKEN);

      // then
      assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("유효한 리프레시 토큰에서 클레임을 추출한다")
    void shouldExtractClaimFromValidRefreshToken() {
      // given
      String email = "test@example.com";
      String token = jwtTokenProvider.generateRefreshToken(email);

      // when
      String extractedEmail =
          jwtTokenProvider.getClaimFromToken(token, "email", String.class, TokenType.REFRESH_TOKEN);

      // then
      assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("만료된 토큰에서 클레임 추출 시 BusinessException을 발생시킨다")
    void shouldThrowBusinessExceptionWhenTokenIsExpired() {
      // given
      SecretKey key = jwtTokenProvider.createKey(accessTokenSecret);
      String expiredToken =
          Jwts.builder()
              .claim("email", "test@example.com")
              .issuedAt(new Date(System.currentTimeMillis() - 10000))
              .expiration(new Date(System.currentTimeMillis() - 5000)) // 이미 만료됨
              .signWith(key)
              .compact();

      // when & then
      assertThatThrownBy(
              () ->
                  jwtTokenProvider.getClaimFromToken(
                      expiredToken, "email", String.class, TokenType.ACCESS_TOKEN))
          .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("잘못된 형식의 토큰에서 클레임 추출 시 BusinessException을 발생시킨다")
    void shouldThrowBusinessExceptionWhenTokenIsMalformed() {
      // given
      String malformedToken = "invalid.token.format";

      // when & then
      assertThatThrownBy(
              () ->
                  jwtTokenProvider.getClaimFromToken(
                      malformedToken, "email", String.class, TokenType.ACCESS_TOKEN))
          .isInstanceOf(BusinessException.class);
    }
  }

  @Nested
  @DisplayName("validateAccessToken 메서드는")
  class ValidateAccessToken {

    @Test
    @DisplayName("유효한 액세스 토큰을 검증한다")
    void shouldValidateValidAccessToken() {
      // given
      String email = "test@example.com";
      String token = jwtTokenProvider.generateAccessToken(email);

      // when & then
      jwtTokenProvider.validateAccessToken(token); // 예외가 발생하지 않아야 함
    }

    @Test
    @DisplayName("만료된 액세스 토큰 검증 시 BusinessException을 발생시킨다")
    void shouldThrowBusinessExceptionWhenAccessTokenIsExpired() {
      // given
      SecretKey key = jwtTokenProvider.createKey(accessTokenSecret);
      String expiredToken =
          Jwts.builder()
              .claim("email", "test@example.com")
              .issuedAt(new Date(System.currentTimeMillis() - 10000))
              .expiration(new Date(System.currentTimeMillis() - 5000))
              .signWith(key)
              .compact();

      // when & then
      assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(expiredToken))
          .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("잘못된 형식의 액세스 토큰 검증 시 BusinessException을 발생시킨다")
    void shouldThrowBusinessExceptionWhenAccessTokenIsMalformed() {
      // given
      String malformedToken = "invalid.token.format";

      // when & then
      assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(malformedToken))
          .isInstanceOf(BusinessException.class);
    }
  }

  @Nested
  @DisplayName("validateToken 메서드는")
  class ValidateToken {

    @Test
    @DisplayName("유효한 토큰과 시크릿키로 검증한다")
    void shouldValidateTokenWithCorrectSecret() {
      // given
      String email = "test@example.com";
      String token = jwtTokenProvider.generateRefreshToken(email);

      // when & then
      jwtTokenProvider.validateToken(token, refreshTokenSecret); // 예외가 발생하지 않아야 함
    }
  }

  @Nested
  @DisplayName("createExpiryDate 메서드는")
  class CreateExpiryDate {

    @Test
    @DisplayName("현재 시간부터 지정된 시간만큼 후의 Date를 생성한다")
    void shouldCreateExpiryDateFromNow() {
      // given
      long expirationTime = 3600000L; // 1 hour

      // when
      Date expiryDate = jwtTokenProvider.createExpiryDate(expirationTime);

      // then
      assertThat(expiryDate).isNotNull();
      assertThat(expiryDate.getTime())
          .isGreaterThan(System.currentTimeMillis())
          .isLessThanOrEqualTo(System.currentTimeMillis() + expirationTime + 1000);
    }
  }

  @Nested
  @DisplayName("createKey 메서드는")
  class CreateKey {

    @Test
    @DisplayName("Base64 인코딩된 시크릿으로부터 SecretKey를 생성한다")
    void shouldCreateSecretKeyFromBase64EncodedSecret() {
      // given
      String secret = accessTokenSecret;

      // when
      SecretKey secretKey = jwtTokenProvider.createKey(secret);

      // then
      assertThat(secretKey).isNotNull();
      assertThat(secretKey.getAlgorithm()).isEqualTo("HmacSHA256");

      byte[] expectedKeyBytes = Decoders.BASE64URL.decode(secret);
      SecretKey expectedKey = Keys.hmacShaKeyFor(expectedKeyBytes);

      assertThat(secretKey.getEncoded()).isEqualTo(expectedKey.getEncoded());
    }
  }
}
