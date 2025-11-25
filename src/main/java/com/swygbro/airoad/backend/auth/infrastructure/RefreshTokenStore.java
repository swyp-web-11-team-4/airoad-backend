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

      log.info(
          "[REDIS] RefreshToken 저장 시작 - emailKey: {}, tokenKey: {}, TTL: {}초",
          emailKey,
          tokenKey,
          ttlSeconds);

      stringRedisTemplate.opsForValue().set(emailKey, value, ttlSeconds, TimeUnit.SECONDS);
      stringRedisTemplate.opsForValue().set(tokenKey, value, ttlSeconds, TimeUnit.SECONDS);

      log.info("[REDIS] RefreshToken 저장 완료 - emailHash: {}", redisRefreshToken.getEmailHash());
    } catch (Exception e) {
      log.error("[REDIS] RefreshToken 저장 실패", e);
      throw new IllegalStateException("refresh 토큰 Redis 저장 실패", e);
    }
  }

  // 이메일 해시로 조회
  public Optional<RedisRefreshToken> findByEmailHash(String emailHash) {
    String emailKey = EMAIL_KEY_PREFIX + emailHash;
    log.info("[REDIS] RefreshToken 조회 시도 - emailKey: {}", emailKey);

    String value = stringRedisTemplate.opsForValue().get(emailKey);
    if (value == null) {
      log.info("[REDIS] RefreshToken 조회 결과: 없음 - emailHash: {}", emailHash);
      return Optional.empty();
    }
    try {
      RedisRefreshToken token = objectMapper.readValue(value, RedisRefreshToken.class);
      log.info("[REDIS] RefreshToken 조회 성공 - emailHash: {}", emailHash);
      return Optional.of(token);
    } catch (Exception e) {
      log.error("[REDIS] RefreshToken 역직렬화 실패 - emailHash: {}", emailHash, e);
      return Optional.empty();
    }
  }

  // 토큰 해시로 조회
  public Optional<RedisRefreshToken> findByTokenHash(String tokenHash) {
    String tokenKey = TOKEN_KEY_PREFIX + tokenHash;
    log.info("[REDIS] RefreshToken 조회 시도 - tokenKey: {}", tokenKey);

    String value = stringRedisTemplate.opsForValue().get(tokenKey);
    if (value == null) {
      log.info("[REDIS] RefreshToken 조회 결과: 없음 - tokenHash: {}", tokenHash);
      return Optional.empty();
    }
    try {
      RedisRefreshToken token = objectMapper.readValue(value, RedisRefreshToken.class);
      log.info("[REDIS] RefreshToken 조회 성공 - tokenHash: {}", tokenHash);
      return Optional.of(token);
    } catch (Exception e) {
      log.error("[REDIS] RefreshToken 역직렬화 실패 - tokenHash: {}", tokenHash, e);
      return Optional.empty();
    }
  }

  // 이메일로 해시 삭제
  public void deleteByEmailHash(String emailHash) {
    log.info("[REDIS] RefreshToken 삭제 시작 - emailHash: {}", emailHash);

    findByEmailHash(emailHash)
        .ifPresent(
            redisRefreshToken -> {
              String emailKey = EMAIL_KEY_PREFIX + emailHash;
              String tokenKey = TOKEN_KEY_PREFIX + redisRefreshToken.getTokenHash();

              log.info(
                  "[REDIS] RefreshToken 삭제 실행 - emailKey: {}, tokenKey: {}", emailKey, tokenKey);

              stringRedisTemplate.delete(emailKey);
              stringRedisTemplate.delete(tokenKey);

              log.info("[REDIS] RefreshToken 삭제 완료 - emailHash: {}", emailHash);
            });
  }

  // 토큰 해시로 삭제
  public void deleteByTokenHash(String tokenHash) {
    log.info("[REDIS] RefreshToken 삭제 시작 - tokenHash: {}", tokenHash);

    findByTokenHash(tokenHash)
        .ifPresent(
            redisRefreshToken -> {
              String emailKey = EMAIL_KEY_PREFIX + redisRefreshToken.getEmailHash();
              String tokenKey = TOKEN_KEY_PREFIX + tokenHash;

              log.info(
                  "[REDIS] RefreshToken 삭제 실행 - emailKey: {}, tokenKey: {}", emailKey, tokenKey);

              stringRedisTemplate.delete(emailKey);
              stringRedisTemplate.delete(tokenKey);

              log.info("[REDIS] RefreshToken 삭제 완료 - tokenHash: {}", tokenHash);
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
