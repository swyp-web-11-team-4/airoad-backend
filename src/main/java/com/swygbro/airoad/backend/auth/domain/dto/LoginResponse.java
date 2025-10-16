package com.swygbro.airoad.backend.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swygbro.airoad.backend.member.domain.dto.MemberResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "LoginResponse", description = "소셜 로그인 성공 응답")
public class LoginResponse {

  private TokenResponse tokenInfo;
  private MemberResponse userInfo;

  public static LoginResponse of(MemberResponse memberInfo, TokenResponse tokenInfo) {
    return LoginResponse.builder().tokenInfo(tokenInfo).userInfo(memberInfo).build();
  }
}
