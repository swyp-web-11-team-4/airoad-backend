package com.swygbro.airoad.backend.auth.domain.dto.oauth2;

import java.util.Map;

public interface OAuth2UserInfo {
  String getId();

  String getName();

  String getEmail();

  String getImageUrl();

  Map<String, Object> attributes();
}
