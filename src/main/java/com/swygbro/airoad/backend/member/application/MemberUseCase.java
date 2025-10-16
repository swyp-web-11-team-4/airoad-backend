package com.swygbro.airoad.backend.member.application;

import com.swygbro.airoad.backend.member.domain.dto.MemberResponse;

public interface MemberUseCase {
  MemberResponse getMemberById(Long id);

  MemberResponse getMemberByEmail(String email);
}
