package com.swygbro.airoad.backend.ai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ChatMemoryConfig {

  @Bean
  public ChatMemoryRepository chatMemoryRepository(JdbcTemplate jdbcTemplate) {
    return JdbcChatMemoryRepository.builder()
        .jdbcTemplate(jdbcTemplate)
        .dialect(new CustomChatMemoryRepositoryDialect())
        .build();
  }

  @Bean
  public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
    return MessageWindowChatMemory.builder()
        .chatMemoryRepository(chatMemoryRepository)
        .maxMessages(10)
        .build();
  }
}

class CustomChatMemoryRepositoryDialect implements JdbcChatMemoryRepositoryDialect {

  /** 특정 대화(conversation)의 메시지 목록을 조회하는 SQL */
  @Override
  public String getSelectMessagesSql() {
    return """
            SELECT
                m.content,
                m.message_type
            FROM ai_message AS m
            INNER JOIN ai_conversation AS c ON m.conversation_id = c.id
            WHERE c.id = CAST(? AS BIGINT)
            ORDER BY m.created_at
            """;
  }

  /** 새 메시지를 삽입하는 SQL */
  @Override
  public String getInsertMessageSql() {
    return """
            INSERT INTO ai_message (conversation_id, content, message_type, created_at, updated_at)
            VALUES (
                CAST(? AS BIGINT),
                ?,
                CAST(? AS VARCHAR),
                ?,
                NOW()
            )
            """;
  }

  /** 모든 대화 ID 목록을 조회하는 SQL */
  @Override
  public String getSelectConversationIdsSql() {
    return """
            SELECT DISTINCT CAST(id AS VARCHAR) AS conversation_id
            FROM ai_conversation
            ORDER BY created_at
            """;
  }

  /** 특정 대화의 모든 메시지를 삭제하는 SQL */
  @Override
  public String getDeleteMessagesSql() {
    return """
            DELETE FROM ai_message
            WHERE conversation_id = CAST(? AS BIGINT)
            """;
  }
}
