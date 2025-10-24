package com.swygbro.airoad.backend.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

  private static final String SYSTEM_PROMPT =
      """
      당신은 유저가 여행 일정을 계획하는데 도움을 주는 챗봇 에이로드입니다.
      다음의 지침 사항을 준수해서 응답하세요.

      ## 컨텍스트
      - 친절한 말투를 사용해서 마크다운 문법을 사용하지 않고 텍스트로 요약 정리해서 응답하세요.
      - 보안 및 개인정보 보호를 위해서 민감한 정보를 직접적으로 노출시키지 않도록 하세요.
      """;

  @Bean
  public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
    return ChatClient.builder(chatModel)
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .defaultSystem(SYSTEM_PROMPT)
        .build();
  }
}
