package com.swygbro.airoad.backend.common.infrastructure.logging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SensitiveLogMasker {

  private static final String MASK = "***";

  /**
   * 이메일 마스킹: abc@example.com -> abc***@example.com
   *
   * @param email 마스킹할 이메일
   * @return 마스킹된 이메일
   */
  public static String maskEmail(String email) {
    if (email == null || email.isEmpty()) {
      return MASK;
    }

    if (!email.contains("@")) {
      return MASK;
    }

    String[] strings = email.split("@");
    if (strings.length != 2 || strings[0].isEmpty()) {
      return MASK;
    }

    String first = strings[0];
    int visibleLength = first.length() / 2;
    String visible = first.substring(0, Math.max(1, visibleLength));

    return visible + MASK + "@" + strings[1];
  }

  /**
   * JWT 토큰 마스킹: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... -> eyJh***
   *
   * @param token 마스킹할 JWT 토큰
   * @return 마스킹된 토큰
   */
  public static String maskJwtToken(String token) {
    if (token == null || token.isEmpty()) {
      return MASK;
    }

    int visibleLength = Math.min(4, token.length());
    return token.substring(0, visibleLength) + MASK;
  }
}
