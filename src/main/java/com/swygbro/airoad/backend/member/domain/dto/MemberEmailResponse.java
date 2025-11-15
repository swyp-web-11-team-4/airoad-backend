package com.swygbro.airoad.backend.member.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 회원 이메일 응답 DTO */
@Schema(name = "MemberEmailResponse", description = "회원 이메일 응답")
public record MemberEmailResponse(
    @Schema(description = "사용자 이메일", example = "user@example.com") String email) {}
