package com.swygbro.airoad.backend.fixture.member;

import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.enumerated.MemberRole;
import com.swygbro.airoad.backend.member.domain.enumerated.ProviderType;

public class MemberFixture {

  public static Member create() {
    return Member.builder()
        .email("test@example.com")
        .name("테스트 사용자")
        .imageUrl("https://example.com/profile.jpg")
        .provider(ProviderType.GOOGLE)
        .role(MemberRole.MEMBER)
        .build();
  }

  public static Member createAdmin() {
    return Member.builder()
        .email("admin@example.com")
        .name("관리자")
        .imageUrl("https://example.com/admin.jpg")
        .provider(ProviderType.GOOGLE)
        .role(MemberRole.ADMIN)
        .build();
  }

  public static Member createWithEmail(String email) {
    return Member.builder()
        .email(email)
        .name("테스트 사용자")
        .imageUrl("https://example.com/profile.jpg")
        .provider(ProviderType.GOOGLE)
        .role(MemberRole.MEMBER)
        .build();
  }

  public static Member.MemberBuilder builder() {
    return Member.builder()
        .email("test@example.com")
        .name("테스트 사용자")
        .imageUrl("https://example.com/profile.jpg")
        .provider(ProviderType.GOOGLE)
        .role(MemberRole.MEMBER);
  }
}
