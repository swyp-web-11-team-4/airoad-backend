package com.swygbro.airoad.backend.ai.agent.chat;

import com.swygbro.airoad.backend.ai.agent.AiroadAgent;
import com.swygbro.airoad.backend.ai.domain.dto.AiResponseContentType;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatAgent implements AiroadAgent {

  private static final String name = "chatAgent";

  private final ChatClient chatClient;

  public ChatAgent(ChatModel chatModel, ChatMemory chatMemory) {
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultSystem(
                """
                    당신은 AI 여행 일정 추천 서비스의 챗봇 Airoad 입니다.
                    유저의 요구사항에 대해 친절하게 응답하세요.
                    """)
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
            .build();
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public boolean supports(AiResponseContentType type) {
    return type.equals(AiResponseContentType.CHAT);
  }

  @Override
  public CommonResponse<String> execute(Object data) {
    return null;
  }
}
