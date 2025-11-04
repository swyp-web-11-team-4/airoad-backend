package com.swygbro.airoad.backend.auth.domain.dto.oauth2;

import java.util.Map;

public class OAuth2UserInfoFactory {

  public static OAuth2UserInfo extractOAuth2UserInfo(
      String registrationId, Map<String, Object> attributes) {
    return switch (registrationId.toLowerCase()) {
      case "google" -> new GoogleUserInfo(attributes);
        // 추가 OAuth2 제공자는 여기에 추가하세요.
      default -> throw new IllegalArgumentException("지원하지 않는 OAuth2 제공자입니다: " + registrationId);
    };
  }
}
