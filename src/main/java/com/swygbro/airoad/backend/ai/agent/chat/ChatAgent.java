package com.swygbro.airoad.backend.ai.agent.chat;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.chat.dto.request.AiChatRequest;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor.MetadataEntry;
import com.swygbro.airoad.backend.ai.common.agent.AiroadAgent;
import com.swygbro.airoad.backend.ai.common.context.ContextManager;
import com.swygbro.airoad.backend.ai.domain.dto.context.ChatRoomContext;
import com.swygbro.airoad.backend.ai.domain.dto.context.TripPlanQueryContext;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.event.AiMessageGeneratedEvent;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.application.PlaceQueryUseCase;
import com.swygbro.airoad.backend.trip.application.ScheduledPlaceCommandUseCase;

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

  public ChatAgent(
      @Qualifier("openAiChatModel") ChatModel chatModel,
      VectorStore vectorStore,
      ChatMemory chatMemory,
      ApplicationEventPublisher eventPublisher,
      PlaceQueryUseCase placeQueryUseCase,
      ScheduledPlaceCommandUseCase scheduledPlaceCommandUseCase,
      ContextManager contextManager) {
    this.eventPublisher = eventPublisher;
    this.contextManager = contextManager;
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultAdvisors(
                new SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                PromptMetadataAdvisor.builder().build(),
                QuestionAnswerAdvisor.builder(vectorStore)
                    .searchRequest(
                        SearchRequest.builder().similarityThreshold(0.5d).topK(25).build())
                    .build())
            .defaultTools(placeQueryUseCase, scheduledPlaceCommandUseCase)
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
          contextManager.buildContext(AgentType.CHAT_AGENT, tripPlanQueryContext, chatRoomContext);

      String response =
          chatClient
              .prompt()
              .user(request.userPrompt())
              .advisors(
                  a ->
                      a.param(ChatMemory.CONVERSATION_ID, request.chatRoomId())
                          .param(PromptMetadataAdvisor.METADATA_KEY, contextMetadata))
              .call()
              .content();

      log.debug("ChatAgent 응답 생성 완료 - response: {}", response);

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
