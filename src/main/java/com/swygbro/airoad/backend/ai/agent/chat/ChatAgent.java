package com.swygbro.airoad.backend.ai.agent.chat;

import com.swygbro.airoad.backend.ai.agent.advisor.PromptMetadataAdvisor;
import com.swygbro.airoad.backend.ai.agent.chat.dto.request.AiChatRequest;
import com.swygbro.airoad.backend.ai.agent.common.AbstractPromptAgent;
import com.swygbro.airoad.backend.ai.application.query.AiPromptTemplateQueryUseCase;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.event.AiMessageGeneratedEvent;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.application.PlaceQueryUseCase;
import com.swygbro.airoad.backend.trip.application.ScheduledPlaceCommandUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * AI 채팅 에이전트
 *
 * <p>사용자의 자연어 요청을 분석하여 여행 일정 조회 및 수정 기능을 제공합니다.
 *
 * <p>도메인별 Application Service를 주입받아 @Tool 메서드를 자동으로 등록합니다.
 */
@Slf4j
@Component
public class ChatAgent extends AbstractPromptAgent {

  private final AgentType agentType = AgentType.CHAT_AGENT;

  private final ChatClient chatClient;
  private final ApplicationEventPublisher eventPublisher;

  public ChatAgent(
      @Qualifier("upstageChatModel") ChatModel chatModel,
      VectorStore vectorStore,
      ChatMemory chatMemory,
      ApplicationEventPublisher eventPublisher,
      PlaceQueryUseCase placeQueryUseCase,
      ScheduledPlaceCommandUseCase scheduledPlaceCommandUseCase,
      AiPromptTemplateQueryUseCase promptTemplateQueryUseCase) {
    super(promptTemplateQueryUseCase);
    this.eventPublisher = eventPublisher;
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultAdvisors(
//                new SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                PromptMetadataAdvisor.builder().build()
                //                QuestionAnswerAdvisor.builder(vectorStore)
                //                    .searchRequest(
                //
                // SearchRequest.builder().similarityThreshold(0.5d).topK(5).build())
                //                    .build()
                )
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

      String systemPrompt = findActiveSystemPrompt(agentType);

      String response =
          chatClient
              .prompt()
              .system(systemPrompt)
              .user(request.userPrompt())
              .advisors(
                  a ->
                      a.param(ChatMemory.CONVERSATION_ID, request.chatRoomId())
                          .param(
                              PromptMetadataAdvisor.METADATA_KEY,
                              PromptMetadataAdvisor.userMetadata(
                                  """
                          ### 채팅방 정보
                          chatRoomId: %s
                          tripPlanId: %s
                          username: %s
                          """
                                      .formatted(
                                          request.chatRoomId(),
                                          request.tripPlanId(),
                                          request.username()))))
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
      log.debug(e.getMessage(), e);
      throw new BusinessException(
          AiErrorCode.AGENT_EXECUTION_FAILED,
          "ChatAgent 실행 중 오류가 발생했습니다: %s".formatted(e.getMessage()));
    }
  }
}
