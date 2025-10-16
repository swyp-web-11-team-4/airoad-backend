package com.swygbro.airoad.backend.member.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.common.exception.BusinessException;
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

  @Override
  public MemberResponse getMemberById(Long id) {
    Member member =
        memberRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

    log.info("[회원 조회 성공] id: {}, email: {}", member.getId(), member.getEmail());
    return MemberResponse.from(member);
  }

  @Override
  public MemberResponse getMemberByEmail(String email) {
    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

    log.info("[회원 조회 성공] id: {}, email: {}", member.getId(), member.getEmail());
    return MemberResponse.from(member);
  }
}
