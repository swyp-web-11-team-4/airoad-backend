package com.swygbro.airoad.backend.member.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;

public interface MemberRepository extends JpaRepository<Member, Long> {
  Optional<Member> findByEmailAndProvider(
      @Param("email") String email, @Param("provider") ProviderType provider);

  Optional<Member> findByEmail(String email);
}
