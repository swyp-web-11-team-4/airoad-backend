package com.swygbro.airoad.backend.ai.agent.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.AiroadAgent;
import com.swygbro.airoad.backend.ai.domain.dto.AiResponseContentType;
import com.swygbro.airoad.backend.ai.domain.event.AiMessageGeneratedEvent;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.chat.domain.event.AiChatRequestedEvent;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;

/**
 * AI 채팅 에이전트
 *
 * <p>사용자의 자연어 요청을 분석하여 여행 일정 조회 및 수정 기능을 제공합니다.
 *
 * <p>도메인별 Application Service를 주입받아 @Tool 메서드를 자동으로 등록합니다.
 */
@Slf4j
@Component
public class ChatAgent implements AiroadAgent {

  private static final String name = "chatAgent";

  private static final String SYSTEM_PROMPT_TEMPLATE =
      """
          당신은 AI 여행 일정 추천 서비스의 챗봇 Airoad 입니다.
          유저의 요청을 분석하여 적절한 Tool을 호출하세요.

          사용 가능한 기능:
          - 일정 요약 (전체 또는 특정 일차)
          - 일정 수정 (향후 추가 예정)
          """;

  private final ChatClient chatClient;
  private final ApplicationEventPublisher eventPublisher;

  public ChatAgent(
      ChatModel chatModel, ChatMemory chatMemory, ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultSystem(SYSTEM_PROMPT_TEMPLATE)
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
  public void execute(Object data) {
    AiChatRequestedEvent event = (AiChatRequestedEvent) data;

    try {
      log.debug(
          "ChatAgent 실행 - chatRoomId: {}, tripPlanId: {}, username: {}",
          event.chatRoomId(),
          event.tripPlanId(),
          event.username());

      String response = chatClient.prompt()
          .user(event.userMessage())
          .call()
          .content();

      log.debug("ChatAgent 응답 생성 완료 - response: {}", response);

      AiMessageGeneratedEvent generatedEvent =
          AiMessageGeneratedEvent.builder()
              .chatRoomId(event.chatRoomId())
              .tripPlanId(event.tripPlanId())
              .username(event.username())
              .aiMessage(response)
              .build();

      eventPublisher.publishEvent(generatedEvent);

      log.info("ChatAgent 실행 완료 - chatRoomId: {}", event.chatRoomId());

    } catch (Exception e) {
      throw new BusinessException(
          AiErrorCode.AGENT_EXECUTION_FAILED, "ChatAgent 실행 중 오류가 발생했습니다", e);
    }
  }
}
