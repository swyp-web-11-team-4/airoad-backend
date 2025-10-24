package com.swygbro.airoad.backend.auth.domain.dto;

import com.swygbro.airoad.backend.member.domain.entity.Member;

public record Auth(Member member, boolean isNew) {}
