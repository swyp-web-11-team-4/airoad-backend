package com.swygbro.airoad.backend.oauth.domain.info;

import java.util.Map;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class OAuth2UserInfo {
  protected Map<String, Object> attributes;

  public abstract String getId();

  public abstract String getName();

  public abstract String getEmail();

  public abstract String getImageUrl();
}
