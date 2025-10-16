package com.swygbro.airoad.backend.auth.application;

import java.util.Date;

import org.springframework.security.core.Authentication;

import com.swygbro.airoad.backend.member.domain.entity.Member;

public interface TokenUseCase {

  void createRefreshToken(String token, Date expiryDate, Authentication authentication);

  void deleteRefreshToken(String email);

  void deleteRefreshTokenByUser(Member member);
}
