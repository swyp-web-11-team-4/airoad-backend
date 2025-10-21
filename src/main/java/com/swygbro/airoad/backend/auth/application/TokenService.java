package com.swygbro.airoad.backend.auth.application;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.auth.domain.entity.RefreshToken;
import com.swygbro.airoad.backend.auth.exception.AuthErrorCode;
import com.swygbro.airoad.backend.auth.infrastructure.RefreshTokenRepository;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenService implements TokenUseCase {

  @Value("${jwt.refresh-token-expiration}")
  private long refreshTokenExpiration;

  private final RefreshTokenRepository refreshTokenRepository;

  private final MemberRepository memberRepository;

  @Transactional
  public void createRefreshToken(String token, String email) {
    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));

    RefreshToken refreshToken =
        RefreshToken.builder()
            .token(token)
            .member(member)
            .expiryDate(convertToLocalDateTime(refreshTokenExpiration))
            .build();

    refreshTokenRepository.save(refreshToken);
  }

  @Transactional
  public void deleteRefreshTokenByEmail(String email) {
    refreshTokenRepository.findByMemberEmail(email).ifPresent(refreshTokenRepository::delete);
  }

  private LocalDateTime convertToLocalDateTime(Long date) {
    Date from = Date.from(Instant.now().plusMillis(date));
    return Instant.ofEpochMilli(from.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
  }
}
