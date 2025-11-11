package com.swygbro.airoad.backend.ai.agent.summary;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.common.AbstractPromptAgent;
import com.swygbro.airoad.backend.ai.agent.summary.dto.PlaceSummaryAiResponse;
import com.swygbro.airoad.backend.ai.agent.summary.dto.request.AiPlaceSummaryRequest;
import com.swygbro.airoad.backend.ai.application.query.AiPromptTemplateQueryUseCase;
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
public class PlaceSummaryAgent extends AbstractPromptAgent {

  private final AgentType agentType = AgentType.PLACE_SUMMARY_AGENT;

  // TODO: DB 마이그레이션 후 제거 예정 - 현재는 참고용으로 유지
  private static final String SYSTEM_PROMPT_TEMPLATE =
      """
      당신은 여행지 정보를 자연스러운 문단으로 작성하는 전문가입니다.

      레이블, 불릿 포인트, 마크다운 문법을 사용하지 말고, 완전한 문장으로만 구성된 자연스러운 문단을 작성하세요.

      작성 원칙:
      1. 여행 가이드북이나 블로그 리뷰처럼 자연스러운 문체 사용
      2. 지역명(시/도, 시/군/구)을 문단 전체에 자연스럽게 2-3회 반복
      3. 장소의 특징, 위치, 교통 정보를 자연스러운 문장으로 녹여서 표현
      4. "특징:", "위치:" 같은 레이블 사용 금지
      5. 2-3개 문단, 총 150-250단어 분량으로 작성
      """;

  // TODO: DB 마이그레이션 후 제거 예정 - 현재는 참고용으로 유지
  private static final String USER_PROMPT_TEMPLATE =
      """
            다음 장소 정보를 자연어 문장으로 작성해주세요:

            장소명: {name}
            주소: {address}
            설명: {description}
            테마: {themes}

            예시:
            서울 강남구에 위치한 국립중앙박물관은 한국의 역사와 문화를 한눈에 볼 수 있는 대표적인 관광명소입니다. 서울특별시 강남구 테헤란로 123번지에 자리잡고 있으며, 지하철 2호선 강남역 3번 출구에서 도보 5분이면 도착할 수 있습니다.
            이곳은 상설 전시관에서 다양한 유물을 관람할 수 있고, 주말에는 가족 단위 방문객을 위한 특별 프로그램도 운영됩니다. 전통문화를 직접 체험할 수 있는 공간과 전문 해설사의 동행 투어도 마련되어 있습니다. 박물관 내부에는 포토존과 기념품샵도 함께 운영되고 있어 관광과 문화체험을 동시에 즐길 수 있습니다.

            주의사항:
            - 마크다운 사용 금지
            - 지역명(주소의 시/도, 시/군/구)을 자연스럽게 2-3회 반복
            - "특징:", "위치:" 같은 레이블 절대 사용 금지
            - 불릿 포인트(-) 사용 금지
            - 완전한 문장으로만 구성
            """;

  private final ChatClient chatClient;
  private final ApplicationEventPublisher eventPublisher;

  public PlaceSummaryAgent(
      ApplicationEventPublisher eventPublisher,
      OpenAiChatModel upstageChatModel,
      AiPromptTemplateQueryUseCase promptTemplateQueryUseCase) {
    super(promptTemplateQueryUseCase);
    this.eventPublisher = eventPublisher;
    this.chatClient =
        ChatClient.builder(upstageChatModel)
            .defaultSystem(promptSystemSpec -> promptSystemSpec.text(SYSTEM_PROMPT_TEMPLATE))
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
    PromptPair prompts = findActivePromptPair(agentType);

    String userPrompt =
        prompts
            .userPrompt()
            .replace("{name}", request.name())
            .replace("{address}", request.address())
            .replace("{description}", request.description() != null ? request.description() : "")
            .replace("{themes}", String.join(", ", request.themes()));

    PlaceSummaryAiResponse response =
        chatClient
            .prompt()
            .system(prompts.systemPrompt())
            .user(userPrompt)
            .call()
            .entity(PlaceSummaryAiResponse.class);

    log.debug(
        "AI 자연어 content 생성 완료 - placeId: {}, content length: {}",
        request.placeId(),
        response.content().length());

    return response;
  }
}
