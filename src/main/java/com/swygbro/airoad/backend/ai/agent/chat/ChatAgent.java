package com.swygbro.airoad.backend.ai.agent.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.chat.dto.request.AiChatRequest;
import com.swygbro.airoad.backend.ai.agent.common.AbstractPromptAgent;
import com.swygbro.airoad.backend.ai.application.query.AiPromptTemplateQueryUseCase;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.event.AiMessageGeneratedEvent;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.application.PlaceQueryUseCase;

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
public class ChatAgent extends AbstractPromptAgent {

  private final AgentType agentType = AgentType.CHAT_AGENT;

  // TODO: DB 마이그레이션 후 제거 예정 - 현재는 참고용으로 유지
  private static final String SYSTEM_PROMPT_TEMPLATE =
      """
          당신은 AI 여행 일정 추천 서비스 Airoad의 챗봇 어시스턴트입니다.
          사용자의 여행 일정을 함께 만들고, 조회하고, 수정하는 것을 돕습니다.

          제공된 컨텍스트와 Tool을 활용하여 사용자 요청에 응답하세요.

          응답 작성 방법:
          1. 먼저 컨텍스트를 검토하여 관련 정보를 찾으세요
          2. 사용자 질문에 맞는 정보만 선택하여 응답하세요
          3. 제외한 항목이나 선택 이유는 절대 설명하지 마세요

          금지 사항:
          - "다만", "그러나", "제외되었습니다" 같은 필터링 과정 언급 금지
          - Tool 이름이나 기술적 세부사항 언급 금지
          - 컨텍스트 자체에 대한 메타 설명 금지

          정보가 없는 경우에만 "죄송합니다. 해당 정보를 찾을 수 없습니다"라고 답하세요.
          간결하고 친절하게 대화하세요.
          """;

  private final ChatClient chatClient;
  private final ApplicationEventPublisher eventPublisher;

  public ChatAgent(
      @Qualifier("upstageChatModel") ChatModel chatModel,
      VectorStore vectorStore,
      ChatMemory chatMemory,
      ApplicationEventPublisher eventPublisher,
      PlaceQueryUseCase placeQueryUseCase,
      AiPromptTemplateQueryUseCase promptTemplateQueryUseCase) {
    super(promptTemplateQueryUseCase);
    this.eventPublisher = eventPublisher;
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                QuestionAnswerAdvisor.builder(vectorStore)
                    .searchRequest(
                        SearchRequest.builder().similarityThreshold(0.5d).topK(5).build())
                    .build())
            .defaultTools(placeQueryUseCase)
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
              .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.chatRoomId().toString()))
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
