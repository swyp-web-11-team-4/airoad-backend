package com.swygbro.airoad.backend.member.domain.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swygbro.airoad.backend.member.domain.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {}
