package com.swygbro.airoad.backend.member.domain.enumerated;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberRole {
  MEMBER("MEMBER"),

  ADMIN("ADMIN");

  private final String role;
}
