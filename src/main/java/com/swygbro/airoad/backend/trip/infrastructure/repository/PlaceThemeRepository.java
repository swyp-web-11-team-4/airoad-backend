package com.swygbro.airoad.backend.trip.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swygbro.airoad.backend.content.domain.entity.PlaceTheme;

/** PlaceTheme 엔티티의 JPA Repository */
public interface PlaceThemeRepository extends JpaRepository<PlaceTheme, Long> {}
