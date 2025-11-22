package com.swygbro.airoad.backend.member.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.common.infrastructure.encryption.SHA256Hasher;
import com.swygbro.airoad.backend.member.domain.dto.MemberResponse;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.exception.MemberErrorCode;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService implements MemberUseCase {

  private final MemberRepository memberRepository;
  private final SHA256Hasher sha256Hasher;

  @Override
  public MemberResponse getMemberByEmail(String email) {
    String emailHash = sha256Hasher.hash(email);
    Member member =
        memberRepository
            .findByEmailHash(emailHash)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

    return MemberResponse.from(member);
  }
}
