package com.swygbro.airoad.backend.common.config;

import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;

import lombok.extern.slf4j.Slf4j;

/**
 * 암호화 설정 Nimbus JOSE JWT의 DirectEncrypter/DirectDecrypter를 Bean으로 등록합니다. AES-256-GCM 알고리즘을 사용하여 DB
 * 민감 정보를 암호화합니다.
 */
@Slf4j
@Configuration
public class EncryptionConfig {

  @Value("${encryption.secret-key}")
  private String secretKey;

  /**
   * JWE Direct Encryption을 위한 암호화기 Bean AES-256-GCM 알고리즘 사용
   *
   * @return DirectEncrypter 인스턴스
   */
  @Bean
  public DirectEncrypter directEncrypter() {
    try {
      SecretKey key = generateSecretKey(secretKey);
      log.info("DirectEncrypter Bean 생성 완료 (AES-256-GCM)");
      return new DirectEncrypter(key);
    } catch (Exception e) {
      log.error("DirectEncrypter 생성 실패", e);
      throw new IllegalStateException("암호화기 초기화 실패", e);
    }
  }

  /**
   * JWE Direct Decryption을 위한 복호화기 Bean AES-256-GCM 알고리즘 사용
   *
   * @return DirectDecrypter 인스턴스
   */
  @Bean
  public DirectDecrypter directDecrypter() {
    try {
      SecretKey key = generateSecretKey(secretKey);
      log.info("DirectDecrypter Bean 생성 완료 (AES-256-GCM)");
      return new DirectDecrypter(key);
    } catch (Exception e) {
      log.error("DirectDecrypter 생성 실패", e);
      throw new IllegalStateException("복호화기 초기화 실패", e);
    }
  }

  /**
   * 문자열 비밀키를 256비트 AES SecretKey로 변환 PBKDF2-HMAC-SHA256을 사용하여 안전하게 키 유도
   *
   * @param secretKey 환경변수에서 주입받은 비밀키 문자열
   * @return 256비트 AES SecretKey
   */
  private SecretKey generateSecretKey(String secretKey) {
    try {
      // PBKDF2로 키 유도 (반복 횟수 310000은 OWASP 2023 권장)
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      byte[] salt = "AiroadStaticSalt2024".getBytes(StandardCharsets.UTF_8);
      KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt, 310000, 256);
      SecretKey temp = factory.generateSecret(spec);
      byte[] keyBytes = temp.getEncoded();
      return new SecretKeySpec(keyBytes, "AES");
    } catch (Exception e) {
      throw new IllegalStateException("비밀키 생성 실패", e);
    }
  }
}
