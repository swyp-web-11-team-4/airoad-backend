package com.swygbro.airoad.backend.ai.agent.summary;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.summary.dto.request.AiPlaceSummaryRequest;
import com.swygbro.airoad.backend.ai.agent.summary.dto.response.PlaceSummaryAiResponse;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor.MetadataEntry;
import com.swygbro.airoad.backend.ai.common.agent.AiroadAgent;
import com.swygbro.airoad.backend.ai.common.context.ContextManager;
import com.swygbro.airoad.backend.ai.domain.dto.context.PlaceQueryContext;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.event.PlaceSummaryGeneratedEvent;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;

/**
 * Place 요약 에이전트
 *
 * <p>Tour API의 불규칙한 데이터를 AI로 정제하여 임베딩에 적합한 자연어 문단을 생성합니다.
 *
 * <p>주요 역할:
 *
 * <ul>
 *   <li>자연스러운 문장으로 구성된 순수 텍스트 생성
 *   <li>지역명을 반복 포함하여 검색 정확도 향상
 *   <li>여행 가이드북 스타일의 읽기 쉬운 설명
 * </ul>
 */
@Slf4j
@Component
public class PlaceSummaryAgent implements AiroadAgent {

  private final AgentType agentType = AgentType.PLACE_SUMMARY_AGENT;

  private final ChatClient chatClient;
  private final ApplicationEventPublisher eventPublisher;
  private final ContextManager contextManager;

  public PlaceSummaryAgent(
      ApplicationEventPublisher eventPublisher,
      @Qualifier("naverChatModel") OpenAiChatModel upstageChatModel,
      ContextManager contextManager) {
    this.eventPublisher = eventPublisher;
    this.contextManager = contextManager;
    this.chatClient =
        ChatClient.builder(upstageChatModel)
            .defaultAdvisors(PromptMetadataAdvisor.builder().build())
            .build();
  }

  @Override
  public boolean supports(AgentType agentType) {
    return this.agentType == agentType;
  }

  @Override
  public void execute(Object data) {
    AiPlaceSummaryRequest request = (AiPlaceSummaryRequest) data;

    try {
      log.info("PlaceSummaryAgent 실행 시작 - placeId: {}", request.placeId());

      PlaceSummaryAiResponse response = generateSummary(request);

      PlaceSummaryGeneratedEvent event =
          PlaceSummaryGeneratedEvent.builder()
              .placeId(request.placeId())
              .name(request.name())
              .address(request.address())
              .themes(request.themes())
              .content(response.content())
              .build();

      eventPublisher.publishEvent(event);

      log.info("PlaceSummaryAgent 실행 완료 - placeId: {}", request.placeId());

    } catch (Exception e) {
      log.error("PlaceSummaryAgent 실행 실패 - placeId: {}", request.placeId(), e);
      throw new BusinessException(
          AiErrorCode.AGENT_EXECUTION_FAILED, "PlaceSummaryAgent 실행 중 오류가 발생했습니다", e);
    }
  }

  private PlaceSummaryAiResponse generateSummary(AiPlaceSummaryRequest request) {
    PlaceQueryContext placeQueryContext =
        PlaceQueryContext.builder()
            .name(request.name())
            .address(request.address())
            .description(request.description())
            .operatingHours(request.operatingHours())
            .holidayInfo(request.holidayInfo())
            .themes(request.themes())
            .build();

    List<MetadataEntry> contextMetadata =
        contextManager.buildContext(AgentType.PLACE_SUMMARY_AGENT, placeQueryContext);

    PlaceSummaryAiResponse response =
        chatClient
            .prompt()
            .advisors(a -> a.param(PromptMetadataAdvisor.METADATA_KEY, contextMetadata))
            .call()
            .entity(PlaceSummaryAiResponse.class);

    log.debug(
        "AI 자연어 content 생성 완료 - placeId: {}, content length: {}",
        request.placeId(),
        response.content().length());

    return response;
  }
}
