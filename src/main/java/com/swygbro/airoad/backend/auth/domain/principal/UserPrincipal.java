package com.swygbro.airoad.backend.auth.domain.principal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.swygbro.airoad.backend.member.domain.entity.Member;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPrincipal implements OAuth2User, UserDetails {

  private Member member;
  private Collection<? extends GrantedAuthority> authorities;
  private Map<String, Object> attributes;

  public static UserPrincipal create(Member member) {
    List<GrantedAuthority> authorities =
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));

    return UserPrincipal.builder().member(member).authorities(authorities).build();
  }

  public static UserPrincipal create(Member member, Map<String, Object> attributes) {
    UserPrincipal userPrincipal = UserPrincipal.create(member);

    return UserPrincipal.builder()
        .member(userPrincipal.getMember())
        .authorities(userPrincipal.getAuthorities())
        .attributes(attributes)
        .build();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public String getName() {
    return member.getName();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getUsername() {
    return member.getEmail();
  }

  @Override
  public String getPassword() {
    return null;
  }
}
