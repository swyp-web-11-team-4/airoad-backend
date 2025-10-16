package com.swygbro.airoad.backend.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "소셜 로그인 요청")
public class LoginRequest {

  @Schema(description = "소셜 로그인 제공자", example = "GOOGLE", required = true)
  private ProviderType provider;

  @Schema(description = "제공자의 액세스 토큰", example = "ya29.a0AfH6SMBx...", required = true)
  private String providerAccessToken;
}
