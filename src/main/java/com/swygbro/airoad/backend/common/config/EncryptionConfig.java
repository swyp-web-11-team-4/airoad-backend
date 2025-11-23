package com.swygbro.airoad.backend.common.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.swygbro.airoad.backend.common.infrastructure.encryption.StringEncryptor;

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

  @Bean
  public StringEncryptor stringEncryptor(DirectEncrypter encrypter, DirectDecrypter decrypter) {
    return new StringEncryptor(encrypter, decrypter);
  }

  /**
   * 문자열 비밀키를 256비트 AES SecretKey로 변환 SHA-256 해시를 사용하여 고정된 256비트 키 생성
   *
   * @param secretKey 환경변수에서 주입받은 비밀키 문자열
   * @return 256비트 AES SecretKey
   */
  private SecretKey generateSecretKey(String secretKey) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] keyBytes = digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
      return new SecretKeySpec(keyBytes, "AES");
    } catch (Exception e) {
      throw new IllegalStateException("비밀키 생성 실패", e);
    }
  }
}
