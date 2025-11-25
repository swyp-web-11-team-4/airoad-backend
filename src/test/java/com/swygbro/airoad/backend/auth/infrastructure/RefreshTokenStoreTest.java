package com.swygbro.airoad.backend.auth.infrastructure;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swygbro.airoad.backend.auth.application.JwtTokenProvider;
import com.swygbro.airoad.backend.auth.domain.dto.refreshToken.RedisRefreshToken;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenStore 테스트")
class RefreshTokenStoreTest {

  @Mock private RedisTemplate<String, String> stringRedisTemplate;

  @Mock private ValueOperations<String, String> valueOperations;

  @Mock private ObjectMapper objectMapper;

  @Mock private JwtTokenProvider jwtTokenProvider;

  @InjectMocks private RefreshTokenStore refreshTokenStore;

  private static final String TEST_EMAIL = "encrypted_test@example.com";
  private static final String TEST_EMAIL_HASH = "hash_test@example.com";
  private static final String TEST_TOKEN = "encrypted_test.refresh.token";
  private static final String TEST_TOKEN_HASH = "hash_test.refresh.token";
  private static final String EMAIL_KEY = "auth:refresh_token:emailHash:" + TEST_EMAIL_HASH;
  private static final String TOKEN_KEY = "auth:refresh_token:tokenHash:" + TEST_TOKEN_HASH;
  private static final long TTL_SECONDS = 2592000L; // 30 days

  private RedisRefreshToken testToken;

  @BeforeEach
  void setUp() {
    testToken =
        RedisRefreshToken.builder()
            .email(TEST_EMAIL)
            .emailHash(TEST_EMAIL_HASH)
            .token(TEST_TOKEN)
            .tokenHash(TEST_TOKEN_HASH)
            .build();
  }

  @Nested
  @DisplayName("RefreshToken을 저장할 때")
  class Save {

    @Test
    @DisplayName("이메일과 토큰 두 개의 키로 저장된다")
    void savesWithTwoKeys() throws JsonProcessingException {
      // given
      given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
      String jsonValue = "{\"email\":\"" + TEST_EMAIL + "\"}";
      given(objectMapper.writeValueAsString(testToken)).willReturn(jsonValue);
      given(jwtTokenProvider.getRefreshTokenValidityInSeconds()).willReturn(TTL_SECONDS);

      // when
      refreshTokenStore.save(testToken);

      // then
      verify(valueOperations).set(EMAIL_KEY, jsonValue, TTL_SECONDS, TimeUnit.SECONDS);
      verify(valueOperations).set(TOKEN_KEY, jsonValue, TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("JwtTokenProvider에서 자동으로 TTL을 가져온다")
    void automaticallyGetsTTLFromJwtTokenProvider() throws JsonProcessingException {
      // given
      given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
      String jsonValue = "{}";
      given(objectMapper.writeValueAsString(testToken)).willReturn(jsonValue);
      given(jwtTokenProvider.getRefreshTokenValidityInSeconds()).willReturn(TTL_SECONDS);

      // when
      refreshTokenStore.save(testToken);

      // then
      verify(jwtTokenProvider).getRefreshTokenValidityInSeconds();
      verify(valueOperations, times(2))
          .set(anyString(), anyString(), eq(TTL_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("JSON 직렬화 실패 시 예외가 발생한다")
    void throwsExceptionWhenSerializationFails() throws JsonProcessingException {
      // given
      given(objectMapper.writeValueAsString(testToken))
          .willThrow(new JsonProcessingException("Serialization error") {});

      // when & then
      assertThatThrownBy(() -> refreshTokenStore.save(testToken))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("refresh 토큰 Redis 저장 실패");
    }
  }

  @Nested
  @DisplayName("이메일 해시로 조회할 때")
  class FindByEmailHash {

    @Test
    @DisplayName("저장된 토큰을 반환한다")
    void returnsStoredToken() throws JsonProcessingException {
      // given
      given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
      String jsonValue = "{\"email\":\"" + TEST_EMAIL + "\"}";
      given(valueOperations.get(EMAIL_KEY)).willReturn(jsonValue);
      given(objectMapper.readValue(jsonValue, RedisRefreshToken.class)).willReturn(testToken);

      // when
      Optional<RedisRefreshToken> result = refreshTokenStore.findByEmailHash(TEST_EMAIL_HASH);

      // then
      assertThat(result).isPresent();
      assertThat(result.get()).isEqualTo(testToken);
    }

    @Test
    @DisplayName("토큰이 없으면 Optional.empty()를 반환한다")
    void returnsEmptyWhenNotFound() {
      // given
      given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
      given(valueOperations.get(EMAIL_KEY)).willReturn(null);

      // when
      Optional<RedisRefreshToken> result = refreshTokenStore.findByEmailHash(TEST_EMAIL_HASH);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("JSON 역직렬화 실패 시 Optional.empty()를 반환한다")
    void returnsEmptyWhenDeserializationFails() throws JsonProcessingException {
      // given
      given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
      String jsonValue = "invalid json";
      given(valueOperations.get(EMAIL_KEY)).willReturn(jsonValue);
      given(objectMapper.readValue(jsonValue, RedisRefreshToken.class))
          .willThrow(new JsonProcessingException("Deserialization error") {});

      // when
      Optional<RedisRefreshToken> result = refreshTokenStore.findByEmailHash(TEST_EMAIL_HASH);

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("토큰 해시로 조회할 때")
  class FindByTokenHash {

    @Test
    @DisplayName("저장된 토큰을 반환한다")
    void returnsStoredToken() throws JsonProcessingException {
      // given
      given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
      String jsonValue = "{\"token\":\"" + TEST_TOKEN + "\"}";
      given(valueOperations.get(TOKEN_KEY)).willReturn(jsonValue);
      given(objectMapper.readValue(jsonValue, RedisRefreshToken.class)).willReturn(testToken);

      // when
      Optional<RedisRefreshToken> result = refreshTokenStore.findByTokenHash(TEST_TOKEN_HASH);

      // then
      assertThat(result).isPresent();
      assertThat(result.get()).isEqualTo(testToken);
    }

    @Test
    @DisplayName("토큰이 없으면 Optional.empty()를 반환한다")
    void returnsEmptyWhenNotFound() {
      // given
      given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
      given(valueOperations.get(TOKEN_KEY)).willReturn(null);

      // when
      Optional<RedisRefreshToken> result = refreshTokenStore.findByTokenHash(TEST_TOKEN_HASH);

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("이메일 해시로 삭제할 때")
  class DeleteByEmailHash {

    @Test
    @DisplayName("이메일과 토큰 두 개의 키가 모두 삭제된다")
    void deletesBothKeys() throws JsonProcessingException {
      // given
      given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
      String jsonValue = "{}";
      given(valueOperations.get(EMAIL_KEY)).willReturn(jsonValue);
      given(objectMapper.readValue(jsonValue, RedisRefreshToken.class)).willReturn(testToken);

      // when
      refreshTokenStore.deleteByEmailHash(TEST_EMAIL_HASH);

      // then
      verify(stringRedisTemplate).delete(EMAIL_KEY);
      verify(stringRedisTemplate).delete(TOKEN_KEY);
    }

    @Test
    @DisplayName("토큰이 존재하지 않으면 삭제 작업을 수행하지 않는다")
    void doesNothingWhenTokenNotFound() {
      // given
      given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
      given(valueOperations.get(EMAIL_KEY)).willReturn(null);

      // when
      refreshTokenStore.deleteByEmailHash(TEST_EMAIL_HASH);

      // then
      verify(stringRedisTemplate, never()).delete(anyString());
    }
  }

  @Nested
  @DisplayName("토큰 해시로 삭제할 때")
  class DeleteByTokenHash {

    @Test
    @DisplayName("이메일과 토큰 두 개의 키가 모두 삭제된다")
    void deletesBothKeys() throws JsonProcessingException {
      // given
      given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
      String jsonValue = "{}";
      given(valueOperations.get(TOKEN_KEY)).willReturn(jsonValue);
      given(objectMapper.readValue(jsonValue, RedisRefreshToken.class)).willReturn(testToken);

      // when
      refreshTokenStore.deleteByTokenHash(TEST_TOKEN_HASH);

      // then
      verify(stringRedisTemplate).delete(EMAIL_KEY);
      verify(stringRedisTemplate).delete(TOKEN_KEY);
    }

    @Test
    @DisplayName("토큰이 존재하지 않으면 삭제 작업을 수행하지 않는다")
    void doesNothingWhenTokenNotFound() {
      // given
      given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
      given(valueOperations.get(TOKEN_KEY)).willReturn(null);

      // when
      refreshTokenStore.deleteByTokenHash(TEST_TOKEN_HASH);

      // then
      verify(stringRedisTemplate, never()).delete(anyString());
    }
  }

  @Nested
  @DisplayName("존재 여부를 확인할 때")
  class ExistsBy {

    @Test
    @DisplayName("이메일 해시로 존재 여부를 확인한다")
    void checksByEmailHash() {
      // given
      given(stringRedisTemplate.hasKey(EMAIL_KEY)).willReturn(true);

      // when
      boolean exists = refreshTokenStore.existsByEmailHash(TEST_EMAIL_HASH);

      // then
      assertThat(exists).isTrue();
      verify(stringRedisTemplate).hasKey(EMAIL_KEY);
    }

    @Test
    @DisplayName("토큰 해시로 존재 여부를 확인한다")
    void checksByTokenHash() {
      // given
      given(stringRedisTemplate.hasKey(TOKEN_KEY)).willReturn(true);

      // when
      boolean exists = refreshTokenStore.existsByTokenHash(TEST_TOKEN_HASH);

      // then
      assertThat(exists).isTrue();
      verify(stringRedisTemplate).hasKey(TOKEN_KEY);
    }

    @Test
    @DisplayName("존재하지 않으면 false를 반환한다")
    void returnsFalseWhenNotExists() {
      // given
      given(stringRedisTemplate.hasKey(EMAIL_KEY)).willReturn(false);

      // when
      boolean exists = refreshTokenStore.existsByEmailHash(TEST_EMAIL_HASH);

      // then
      assertThat(exists).isFalse();
    }
  }

  @Nested
  @DisplayName("TTL을 조회할 때")
  class GetTTL {

    @Test
    @DisplayName("남은 TTL을 초 단위로 반환한다")
    void returnsTTLInSeconds() {
      // given
      long expectedTTL = 1000L;
      given(stringRedisTemplate.getExpire(EMAIL_KEY, TimeUnit.SECONDS)).willReturn(expectedTTL);

      // when
      long ttl = refreshTokenStore.getTTL(TEST_EMAIL_HASH);

      // then
      assertThat(ttl).isEqualTo(expectedTTL);
    }

    @Test
    @DisplayName("키가 존재하지 않으면 -2를 반환한다")
    void returnsMinusTwoWhenKeyNotExists() {
      // given
      given(stringRedisTemplate.getExpire(EMAIL_KEY, TimeUnit.SECONDS)).willReturn(-2L);

      // when
      long ttl = refreshTokenStore.getTTL(TEST_EMAIL_HASH);

      // then
      assertThat(ttl).isEqualTo(-2L);
    }

    @Test
    @DisplayName("키에 TTL이 설정되지 않았으면 -1을 반환한다")
    void returnsMinusOneWhenNoTTL() {
      // given
      given(stringRedisTemplate.getExpire(EMAIL_KEY, TimeUnit.SECONDS)).willReturn(-1L);

      // when
      long ttl = refreshTokenStore.getTTL(TEST_EMAIL_HASH);

      // then
      assertThat(ttl).isEqualTo(-1L);
    }
  }
}
