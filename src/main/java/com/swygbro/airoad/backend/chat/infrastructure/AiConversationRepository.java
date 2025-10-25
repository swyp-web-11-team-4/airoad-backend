package com.swygbro.airoad.backend.chat.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {}
