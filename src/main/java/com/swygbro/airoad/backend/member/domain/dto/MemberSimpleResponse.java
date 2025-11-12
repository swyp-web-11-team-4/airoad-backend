package com.swygbro.airoad.backend.member.domain.dto;

import com.swygbro.airoad.backend.member.domain.entity.Member;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record MemberSimpleResponse(
    @Schema(description = "사용자 이름") String name,
    @Schema(description = "사용자 이메일") String email) {

  public static MemberSimpleResponse from(Member member) {
    return MemberSimpleResponse.builder()
        .name(member.getName())
        .email(member.getEmail())
        .build();
  }
}