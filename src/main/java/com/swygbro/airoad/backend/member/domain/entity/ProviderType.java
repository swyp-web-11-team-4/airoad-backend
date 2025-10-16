package com.swygbro.airoad.backend.member.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProviderType {
  GOOGLE("google", "구글");

  private final String providerName;

  private final String displayName;
}
