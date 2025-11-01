package com.swygbro.airoad.backend.auth.domain.info;

import org.springframework.security.oauth2.core.user.OAuth2User;

public class OAuth2UserInfoFactory {

  public static OAuth2UserInfo extractOAuth2UserInfo(String provider, OAuth2User oAuth2User) {
    if ("google".equalsIgnoreCase(provider)) {
      return new GoogleUserInfo(oAuth2User.getAttributes());
    }
    throw new IllegalArgumentException("지원하지 않는 OAuth2 제공자입니다: " + provider);
  }
}
