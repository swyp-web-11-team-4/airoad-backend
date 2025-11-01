package com.swygbro.airoad.backend.auth.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenType {
  ACCESS_TOKEN("access-token"),

  REFRESH_TOKEN("refresh-token"),

  BEARER("Bearer");

  private final String value;
}
