package com.swygbro.airoad.backend.auth.infrastructure;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swygbro.airoad.backend.auth.application.JwtTokenProvider;
import com.swygbro.airoad.backend.auth.domain.dto.refreshToken.RedisRefreshToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {
  private static final String EMAIL_KEY_PREFIX = "auth:refresh_token:emailHash:";
  private static final String TOKEN_KEY_PREFIX = "auth:refresh_token:tokenHash:";

  private final RedisTemplate<String, String> stringRedisTemplate;
  private final ObjectMapper objectMapper;
  private final JwtTokenProvider jwtTokenProvider;

  /**
   * RefreshToken 저장 (이메일과 토큰 양방향 조회 가능)
   *
   * @param redisRefreshToken 저장할 토큰 정보
   */
  public void save(RedisRefreshToken redisRefreshToken) {
    try {
      String value = objectMapper.writeValueAsString(redisRefreshToken);

      String emailKey = EMAIL_KEY_PREFIX + redisRefreshToken.getEmailHash();
      String tokenKey = TOKEN_KEY_PREFIX + redisRefreshToken.getTokenHash();

      long ttlSeconds = jwtTokenProvider.getRefreshTokenValidityInSeconds();

      stringRedisTemplate.opsForValue().set(emailKey, value, ttlSeconds, TimeUnit.SECONDS);
      stringRedisTemplate.opsForValue().set(tokenKey, value, ttlSeconds, TimeUnit.SECONDS);
      log.debug("RefreshToken saved with TTL: {}s", ttlSeconds);
    } catch (Exception e) {
      log.error("Failed to save refresh token", e);
      throw new IllegalStateException("refresh 토큰 Redis 저장 실패", e);
    }
  }

  // 이메일 해시로 조회
  public Optional<RedisRefreshToken> findByEmailHash(String emailHash) {
    String emailKey = EMAIL_KEY_PREFIX + emailHash;
    String value = stringRedisTemplate.opsForValue().get(emailKey);
    if (value == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(objectMapper.readValue(value, RedisRefreshToken.class));
    } catch (Exception e) {
      log.error("Failed to find refresh token by email hash", e);
      return Optional.empty();
    }
  }

  // 토큰 해시로 조회
  public Optional<RedisRefreshToken> findByTokenHash(String tokenHash) {
    String tokenKey = TOKEN_KEY_PREFIX + tokenHash;
    String value = stringRedisTemplate.opsForValue().get(tokenKey);
    if (value == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(objectMapper.readValue(value, RedisRefreshToken.class));
    } catch (Exception e) {
      log.error("Failed to find refresh token by token hash", e);
      return Optional.empty();
    }
  }

  // 이메일로 해시 삭제
  public void deleteByEmailHash(String emailHash) {

    findByEmailHash(emailHash)
        .ifPresent(
            redisRefreshToken -> {
              String emailKey = EMAIL_KEY_PREFIX + emailHash;
              String tokenKey = TOKEN_KEY_PREFIX + redisRefreshToken.getTokenHash();
              stringRedisTemplate.delete(emailKey);
              stringRedisTemplate.delete(tokenKey);
              log.debug("RefreshToken deleted for email hash: {}", emailHash);
            });
  }

  // 토큰 해시로 삭제
  public void deleteByTokenHash(String tokenHash) {

    findByTokenHash(tokenHash)
        .ifPresent(
            redisRefreshToken -> {
              String emailKey = EMAIL_KEY_PREFIX + redisRefreshToken.getEmailHash();
              String tokenKey = TOKEN_KEY_PREFIX + tokenHash;
              stringRedisTemplate.delete(emailKey);
              stringRedisTemplate.delete(tokenKey);
              log.debug("RefreshToken deleted for token hash: {}", tokenHash);
            });
  }

  /**
   * 이메일 해시로 존재 여부 확인
   *
   * @param emailHash 이메일 해시
   * @return 존재 여부
   */
  public boolean existsByEmailHash(String emailHash) {
    String key = EMAIL_KEY_PREFIX + emailHash;
    return stringRedisTemplate.hasKey(key);
  }

  /**
   * 토큰 해시로 존재 여부 확인
   *
   * @param tokenHash 토큰 해시
   * @return 존재 여부
   */
  public boolean existsByTokenHash(String tokenHash) {
    String key = TOKEN_KEY_PREFIX + tokenHash;
    return stringRedisTemplate.hasKey(key);
  }

  /**
   * 남은 TTL 조회 (초)
   *
   * @param emailHash 이메일 해시
   * @return TTL (초), 없으면 -1
   */
  public long getTTL(String emailHash) {
    String key = EMAIL_KEY_PREFIX + emailHash;
    return stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
  }
}
