package com.swygbro.airoad.backend.member.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberRole {
  MEMBER("MEMBER"),

  ADMIN("ADMIN");

  private final String role;
}
