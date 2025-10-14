package com.swygbro.airoad.backend.security.domain.info;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GoogleOAuth2UserInfo implements OAuth2UserInfo {
  private final Map<String, Object> attributes;

  @Override
  public String getId() {
    return (String) attributes.get("sub");
  }

  @Override
  public String getName() {
    return (String) attributes.get("name");
  }

  @Override
  public String getEmail() {
    return (String) attributes.get("email");
  }

  @Override
  public String getImageUrl() {
    return (String) attributes.get("picture");
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }
}
