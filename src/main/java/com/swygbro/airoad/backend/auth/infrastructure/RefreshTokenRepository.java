package com.swygbro.airoad.backend.auth.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swygbro.airoad.backend.auth.domain.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByEmail(String email);

  Optional<RefreshToken> findByEmailHash(String emailHash);

  Optional<RefreshToken> findByToken(String token);

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  void deleteByEmail(String email);

  void deleteByEmailHash(String emailHash);

  boolean existsByEmail(String email);

  boolean existsByEmailHash(String emailHash);
}
