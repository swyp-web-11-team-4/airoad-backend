package com.swygbro.airoad.backend.auth.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swygbro.airoad.backend.auth.domain.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByEmail(String email);

  Optional<RefreshToken> findByToken(String token);

  void deleteByEmail(String email);

  boolean existsByEmail(String email);
}
