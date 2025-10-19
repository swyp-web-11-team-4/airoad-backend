package com.swygbro.airoad.backend.auth.domain.info;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.swygbro.airoad.backend.member.domain.entity.Member;

import lombok.Builder;

@Builder
public record UserPrincipal(
    Member member,
    Collection<? extends GrantedAuthority> authorities,
    Map<String, Object> attributes)
    implements OAuth2User, UserDetails {

  public static UserPrincipal create(Member member) {
    List<GrantedAuthority> authorities =
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));
    return UserPrincipal.builder().member(member).authorities(authorities).build();
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
