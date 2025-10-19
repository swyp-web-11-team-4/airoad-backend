package com.swygbro.airoad.backend.auth.domain.dto;

import com.swygbro.airoad.backend.member.domain.entity.ProviderType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "소셜 로그인 요청")
public record LoginRequest(
    @Schema(description = "소셜 로그인 제공자", example = "GOOGLE", required = true) ProviderType provider,
    @Schema(description = "제공자의 액세스 토큰", example = "ya29.a0AfH6SMBx...", required = true)
        String providerAccessToken) {}
