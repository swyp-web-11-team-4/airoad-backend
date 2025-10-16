package com.swygbro.airoad.backend.auth.application;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.auth.domain.entity.RefreshToken;
import com.swygbro.airoad.backend.auth.infrastructure.RefreshTokenRepository;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenService implements TokenUseCase {

  private final RefreshTokenRepository refreshTokenRepository;
  private final MemberRepository memberRepository;

  @Transactional
  public void createRefreshToken(String token, Date expiryDate, Authentication authentication) {
    String email = authentication.getName();

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

    RefreshToken refreshToken =
        RefreshToken.builder()
            .token(token)
            .member(member)
            .expiryDate(convertToLocalDateTime(expiryDate))
            .build();

    refreshTokenRepository.save(refreshToken);
  }

  @Transactional
  public void deleteRefreshToken(String email) {
    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

    deleteRefreshTokenByUser(member);
  }

  @Transactional
  public void deleteRefreshTokenByUser(Member member) {
    refreshTokenRepository.findByMemberId(member.getId()).ifPresent(refreshTokenRepository::delete);
  }

  private LocalDateTime convertToLocalDateTime(Date date) {
    return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
  }
}
