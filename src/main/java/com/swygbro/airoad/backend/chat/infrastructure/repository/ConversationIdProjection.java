package com.swygbro.airoad.backend.chat.infrastructure.repository;

/** AiConversation ID 프로젝션을 위한 인터페이스 */
public interface ConversationIdProjection {

  Long getTripPlanId();

  Long getConversationId();
}
