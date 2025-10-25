package com.swygbro.airoad.backend.chat.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swygbro.airoad.backend.chat.domain.entity.AiMessage;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {}
