package com.swygbro.airoad.backend.common.infrastructure.encryption;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.swygbro.airoad.backend.common.exception.DecryptionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StringEncryptor 클래스는")
class StringEncryptorTest {

  private StringEncryptor createStringEncryptor() throws Exception {
    byte[] keyBytes = new byte[32]; // 256-bit key
    for (int i = 0; i < keyBytes.length; i++) {
      keyBytes[i] = (byte) i;
    }

    SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
    DirectEncrypter encrypter = new DirectEncrypter(secretKey);
    DirectDecrypter decrypter = new DirectDecrypter(secretKey);

    return new StringEncryptor(encrypter, decrypter);
  }

  @Nested
  @DisplayName("convertToDatabaseColumn 메서드는")
  class ConvertToDatabaseColumn {

    @Test
    @DisplayName("null 입력 시 null을 반환한다")
    void givenNullInput_whenConvert_thenReturnsNull() throws Exception {
      // given
      StringEncryptor encryptor = createStringEncryptor();
      String input = null;

      // when
      String result = encryptor.convertToDatabaseColumn(input);

      // then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("정상 입력 시 암호화된 문자열을 반환한다")
    void givenValidInput_whenConvert_thenReturnsEncryptedString() throws Exception {
      // given
      StringEncryptor encryptor = createStringEncryptor();
      String input = "test@example.com";

      // when
      String result = encryptor.convertToDatabaseColumn(input);

      // then
      assertThat(result).isNotNull();
      assertThat(result).isNotEqualTo(input);
      assertThat(result).startsWith("eyJ"); // JWE format starts with eyJ
    }

    @Test
    @DisplayName("빈 문자열도 암호화할 수 있다")
    void givenEmptyString_whenConvert_thenReturnsEncryptedString() throws Exception {
      // given
      StringEncryptor encryptor = createStringEncryptor();
      String input = "";

      // when
      String result = encryptor.convertToDatabaseColumn(input);

      // then
      assertThat(result).isNotNull();
      assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("같은 입력을 암호화하면 매번 다른 결과를 반환한다")
    void givenSameInput_whenEncryptTwice_thenReturnsDifferentResults() throws Exception {
      // given
      StringEncryptor encryptor = createStringEncryptor();
      String input = "test@example.com";

      // when
      String encrypted1 = encryptor.convertToDatabaseColumn(input);
      String encrypted2 = encryptor.convertToDatabaseColumn(input);

      // then
      assertThat(encrypted1).isNotEqualTo(encrypted2);
    }
  }

  @Nested
  @DisplayName("convertToEntityAttribute 메서드는")
  class ConvertToEntityAttribute {

    @Test
    @DisplayName("null 입력 시 null을 반환한다")
    void givenNullInput_whenConvert_thenReturnsNull() throws Exception {
      // given
      StringEncryptor encryptor = createStringEncryptor();
      String input = null;

      // when
      String result = encryptor.convertToEntityAttribute(input);

      // then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("복호화 실패 시 DecryptionException을 발생시킨다")
    void givenDecryptionFailure_whenConvert_thenThrowsDecryptionException() throws Exception {
      // given
      StringEncryptor encryptor = createStringEncryptor();
      String invalidEncryptedData = "invalid-encrypted-data";

      // when & then
      assertThatThrownBy(() -> encryptor.convertToEntityAttribute(invalidEncryptedData))
          .isInstanceOf(DecryptionException.class)
          .hasMessageContaining("데이터 복호화 실패");
    }
  }

  @Nested
  @DisplayName("암호화 및 복호화 통합 테스트")
  class EncryptionDecryptionIntegration {

    @Test
    @DisplayName("암호화된 데이터를 복호화하면 원본 데이터를 반환한다")
    void givenEncryptedData_whenDecrypt_thenReturnsOriginalData() throws Exception {
      // given
      StringEncryptor encryptor = createStringEncryptor();
      String originalData = "test@example.com";

      // when
      String encryptedData = encryptor.convertToDatabaseColumn(originalData);
      String decryptedData = encryptor.convertToEntityAttribute(encryptedData);

      // then
      assertThat(decryptedData).isEqualTo(originalData);
    }

    @Test
    @DisplayName("한글 데이터를 암호화하고 복호화할 수 있다")
    void givenKoreanData_whenEncryptAndDecrypt_thenReturnsOriginalData() throws Exception {
      // given
      StringEncryptor encryptor = createStringEncryptor();
      String originalData = "안녕하세요@example.com";

      // when
      String encryptedData = encryptor.convertToDatabaseColumn(originalData);
      String decryptedData = encryptor.convertToEntityAttribute(encryptedData);

      // then
      assertThat(decryptedData).isEqualTo(originalData);
    }

    @Test
    @DisplayName("특수문자를 포함한 데이터를 암호화하고 복호화할 수 있다")
    void givenSpecialCharacters_whenEncryptAndDecrypt_thenReturnsOriginalData() throws Exception {
      // given
      StringEncryptor encryptor = createStringEncryptor();
      String originalData = "test+special!@#$%^&*()";

      // when
      String encryptedData = encryptor.convertToDatabaseColumn(originalData);
      String decryptedData = encryptor.convertToEntityAttribute(encryptedData);

      // then
      assertThat(decryptedData).isEqualTo(originalData);
    }

    @Test
    @DisplayName("긴 문자열을 암호화하고 복호화할 수 있다")
    void givenLongString_whenEncryptAndDecrypt_thenReturnsOriginalData() throws Exception {
      // given
      StringEncryptor encryptor = createStringEncryptor();
      String originalData = "a".repeat(1000);

      // when
      String encryptedData = encryptor.convertToDatabaseColumn(originalData);
      String decryptedData = encryptor.convertToEntityAttribute(encryptedData);

      // then
      assertThat(decryptedData).isEqualTo(originalData);
    }

    @Test
    @DisplayName("빈 문자열을 암호화하고 복호화할 수 있다")
    void givenEmptyString_whenEncryptAndDecrypt_thenReturnsOriginalData() throws Exception {
      // given
      StringEncryptor encryptor = createStringEncryptor();
      String originalData = "";

      // when
      String encryptedData = encryptor.convertToDatabaseColumn(originalData);
      String decryptedData = encryptor.convertToEntityAttribute(encryptedData);

      // then
      assertThat(decryptedData).isEqualTo(originalData);
    }
  }
}
