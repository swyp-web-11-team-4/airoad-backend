package com.swygbro.airoad.backend.auth.application;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.swygbro.airoad.backend.auth.domain.dto.UserPrincipal;
import com.swygbro.airoad.backend.common.infrastructure.encryption.SHA256Hasher;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final MemberRepository memberRepository;
  private final SHA256Hasher sha256Hasher;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    String emailHash = sha256Hasher.hash(email);
    Member member =
        memberRepository
            .findByEmailHash(emailHash)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email)); // &&&&

    return new UserPrincipal(member);
  }
}
