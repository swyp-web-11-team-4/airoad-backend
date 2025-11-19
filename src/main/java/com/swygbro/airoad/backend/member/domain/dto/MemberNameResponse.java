package com.swygbro.airoad.backend.member.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 회원 이름 응답 DTO */
@Schema(name = "MemberNameResponse", description = "회원 이름 응답")
public record MemberNameResponse(@Schema(description = "사용자 이름", example = "홍길동") String name) {}
