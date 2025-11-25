package com.swygbro.airoad.backend.common.infrastructure.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SHA256Hasher 클래스는")
class SHA256HasherTest {

  private SHA256Hasher sha256Hasher;

  @BeforeEach
  void setUp() {
    sha256Hasher = new SHA256Hasher();
  }

  @Nested
  @DisplayName("hash 메서드는")
  class Hash {

    @Test
    @DisplayName("null 입력 시 null을 반환한다")
    void givenNullInput_whenHash_thenReturnsNull() {
      // given
      String input = null;

      // when
      String result = sha256Hasher.hash(input);

      // then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("정상 입력 시 64자리 16진수 해시 값을 반환한다")
    void givenValidInput_whenHash_thenReturnsHashString() {
      // given
      String input = "test@example.com";

      // when
      String result = sha256Hasher.hash(input);

      // then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(64); // SHA-256 produces 64 hex characters
      assertThat(result).matches("^[a-f0-9]{64}$"); // Only lowercase hex characters
    }

    @Test
    @DisplayName("동일한 입력에 대해 동일한 해시 값을 반환한다")
    void givenSameInput_whenHash_thenReturnsSameHash() {
      // given
      String input = "test@example.com";

      // when
      String result1 = sha256Hasher.hash(input);
      String result2 = sha256Hasher.hash(input);

      // then
      assertThat(result1).isEqualTo(result2);
    }

    @Test
    @DisplayName("다른 입력에 대해 다른 해시 값을 반환한다")
    void givenDifferentInputs_whenHash_thenReturnsDifferentHashes() {
      // given
      String input1 = "test1@example.com";
      String input2 = "test2@example.com";

      // when
      String result1 = sha256Hasher.hash(input1);
      String result2 = sha256Hasher.hash(input2);

      // then
      assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    @DisplayName("빈 문자열도 해시할 수 있다")
    void givenEmptyString_whenHash_thenReturnsHash() {
      // given
      String input = "";

      // when
      String result = sha256Hasher.hash(input);

      // then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(64);
    }

    @Test
    @DisplayName("알려진 입력에 대해 예상되는 해시 값을 반환한다")
    void givenKnownInput_whenHash_thenReturnsExpectedHash() {
      // given
      String input = "hello";
      // SHA-256 hash of "hello" is known
      String expectedHash = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

      // when
      String result = sha256Hasher.hash(input);

      // then
      assertThat(result).isEqualTo(expectedHash);
    }

    @Test
    @DisplayName("특수문자를 포함한 문자열도 해시할 수 있다")
    void givenSpecialCharacters_whenHash_thenReturnsHash() {
      // given
      String input = "!@#$%^&*()_+-=[]{}|;:',.<>?/~`";

      // when
      String result = sha256Hasher.hash(input);

      // then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(64);
    }

    @Test
    @DisplayName("긴 문자열도 해시할 수 있다")
    void givenLongString_whenHash_thenReturnsHash() {
      // given
      String input = "a".repeat(10000);

      // when
      String result = sha256Hasher.hash(input);

      // then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(64);
    }

    @Test
    @DisplayName("유니코드 문자열도 해시할 수 있다")
    void givenUnicodeString_whenHash_thenReturnsHash() {
      // given
      String input = "안녕하세요 こんにちは 你好";

      // when
      String result = sha256Hasher.hash(input);

      // then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(64);
    }
  }
}
