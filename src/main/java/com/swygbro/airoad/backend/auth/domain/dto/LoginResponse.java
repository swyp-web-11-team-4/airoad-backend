package com.swygbro.airoad.backend.auth.domain.dto;

import com.swygbro.airoad.backend.member.domain.dto.MemberResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "LoginResponse", description = "소셜 로그인 성공 응답")
public record LoginResponse(TokenResponse tokenInfo, MemberResponse userInfo) {

  public static LoginResponse of(MemberResponse memberInfo, TokenResponse tokenInfo) {
    return LoginResponse.builder().tokenInfo(tokenInfo).userInfo(memberInfo).build();
  }
}
