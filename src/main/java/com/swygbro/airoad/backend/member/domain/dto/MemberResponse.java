package com.swygbro.airoad.backend.member.domain.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.swygbro.airoad.backend.member.domain.entity.Member;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberResponse(
    @Schema(description = "사용자 PK") Long id,
    @Schema(description = "사용자 이메일") String email,
    @Schema(description = "사용자 이름") String name,
    @Schema(description = "사용자 프로필 이미지 URL") String imageUrl,
    @Schema(description = "인증 제공자", example = "google") String provider,
    @Schema(description = "사용자 권한", example = "ROLE_USER, ROLE_ADMIN") String role,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "생성 일시", example = "2024-01-01T10:00:00")
        LocalDateTime createdAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "수정 일시", example = "2024-01-01T10:00:00")
        LocalDateTime updatedAt) {

  public static MemberResponse from(Member member) {
    return MemberResponse.builder()
        .id(member.getId())
        .email(member.getEmail())
        .name(member.getName())
        .imageUrl(member.getImageUrl())
        .provider(member.getProvider().getProviderName())
        .role(member.getRole().name())
        .createdAt(member.getCreatedAt())
        .updatedAt(member.getUpdatedAt())
        .build();
  }
}
