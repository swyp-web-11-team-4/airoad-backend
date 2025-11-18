package com.swygbro.airoad.backend.ai.agent.chat;

import com.swygbro.airoad.backend.ai.infrastructure.metrics.TokenUsageOperation;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.chat.dto.request.AiChatRequest;
import com.swygbro.airoad.backend.ai.application.context.dto.ChatRoomContext;
import com.swygbro.airoad.backend.ai.application.context.dto.TripPlanQueryContext;
import com.swygbro.airoad.backend.ai.application.tool.DailyPlanCommandTool;
import com.swygbro.airoad.backend.ai.application.tool.PlaceVectorQueryTool;
import com.swygbro.airoad.backend.ai.application.tool.ScheduledPlaceCommandTool;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor.MetadataEntry;
import com.swygbro.airoad.backend.ai.common.agent.AiroadAgent;
import com.swygbro.airoad.backend.ai.common.context.ContextManager;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.event.AiMessageGeneratedEvent;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.ai.infrastructure.metrics.TokenUsageMetricsService;
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

  private final AgentType agentType = AgentType.CHAT_AGENT;

  private final ChatClient chatClient;
  private final ApplicationEventPublisher eventPublisher;
  private final ContextManager contextManager;
  private final TokenUsageMetricsService tokenUsageMetricsService;

  public ChatAgent(
      @Qualifier("openAiChatModel") ChatModel chatModel,
      ChatMemory chatMemory,
      ApplicationEventPublisher eventPublisher,
      DailyPlanCommandTool dailyPlanCommandTool,
      ScheduledPlaceCommandTool scheduledPlaceCommandTool,
      PlaceVectorQueryTool placeVectorQueryTool,
      ContextManager contextManager,
      TokenUsageMetricsService tokenUsageMetricsService) {
    this.eventPublisher = eventPublisher;
    this.contextManager = contextManager;
    this.tokenUsageMetricsService = tokenUsageMetricsService;
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultAdvisors(
                new SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                PromptMetadataAdvisor.builder().build())
            .defaultTools(dailyPlanCommandTool, scheduledPlaceCommandTool, placeVectorQueryTool)
            .build();
  }

  @Override
  public boolean supports(AgentType agentType) {
    return this.agentType == agentType;
  }

  @Override
  public void execute(Object data) {
    AiChatRequest request = (AiChatRequest) data;

    try {
      log.debug(
          "ChatAgent 실행 - chatRoomId: {}, tripPlanId: {}",
          request.chatRoomId(),
          request.tripPlanId());

      TripPlanQueryContext tripPlanQueryContext =
          TripPlanQueryContext.builder()
              .tripPlanId(request.tripPlanId())
              .username(request.username())
              .build();

      ChatRoomContext chatRoomContext =
          ChatRoomContext.builder()
              .chatRoomId(request.chatRoomId())
              .tripPlanId(request.tripPlanId())
              .username(request.username())
              .build();

      List<MetadataEntry> contextMetadata =
          contextManager.buildContext(AgentType.CHAT_AGENT, chatRoomContext, tripPlanQueryContext);

      ChatResponse chatResponse =
          chatClient
              .prompt()
              .user(request.userPrompt())
              .advisors(
                  a ->
                      a.param(ChatMemory.CONVERSATION_ID, request.chatRoomId())
                          .param(PromptMetadataAdvisor.METADATA_KEY, contextMetadata))
              .call()
              .chatResponse();

      if (chatResponse == null) {
        throw new BusinessException(AiErrorCode.AGENT_EXECUTION_FAILED, "AI 응답을 받지 못했습니다");
      }

      String response = chatResponse.getResult().getOutput().toString();
      Usage usage = chatResponse.getMetadata().getUsage();
      String model = chatResponse.getMetadata().getModel();

      tokenUsageMetricsService.trackTokenUsage(
          request.username(),
          model,
          TokenUsageOperation.CHAT_EDIT,
          usage.getPromptTokens(),
          usage.getCompletionTokens(),
          usage.getTotalTokens());

      log.info(
          "채팅 토큰 사용량 - 사용자: {}, 모델: {}, 입력: {}, 출력: {}, 총: {}",
          request.username(),
          model,
          usage.getPromptTokens(),
          usage.getCompletionTokens(),
          usage.getTotalTokens());

      AiMessageGeneratedEvent generatedEvent =
          AiMessageGeneratedEvent.builder()
              .chatRoomId(request.chatRoomId())
              .tripPlanId(request.tripPlanId())
              .username(request.username())
              .aiMessage(response)
              .build();

      eventPublisher.publishEvent(generatedEvent);

      log.debug("ChatAgent 실행 완료 - chatRoomId: {}", request.chatRoomId());

    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("ChatAgent 실행 중 오류 발생", e);
      throw new BusinessException(
          AiErrorCode.AGENT_EXECUTION_FAILED,
          "ChatAgent 실행 중 오류가 발생했습니다: %s".formatted(e.getMessage()));
    }
  }
}
