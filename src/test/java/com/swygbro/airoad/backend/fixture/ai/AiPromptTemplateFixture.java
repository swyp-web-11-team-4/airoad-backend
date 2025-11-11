package com.swygbro.airoad.backend.fixture.ai;

import com.swygbro.airoad.backend.ai.domain.dto.request.CreateAiPromptTemplateRequest;
import com.swygbro.airoad.backend.ai.domain.dto.request.UpdateAiPromptTemplateRequest;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.entity.AiPromptTemplate;
import com.swygbro.airoad.backend.ai.domain.entity.PromptType;

public class AiPromptTemplateFixture {

  public static AiPromptTemplate createChatAgentSystemPrompt() {
    AiPromptTemplate template =
        AiPromptTemplate.builder()
            .promptType(PromptType.SYSTEM)
            .agentType(AgentType.CHAT_AGENT)
            .prompt("당신은 AI 여행 일정 추천 서비스 Airoad의 챗봇 어시스턴트입니다.")
            .isActive(true)
            .description("ChatAgent의 SYSTEM 프롬프트")
            .build();
    setId(template, 1L);
    return template;
  }

  public static AiPromptTemplate createTripAgentSystemPrompt() {
    AiPromptTemplate template =
        AiPromptTemplate.builder()
            .promptType(PromptType.SYSTEM)
            .agentType(AgentType.TRIP_AGENT)
            .prompt("여행 일정을 생성하는 AI 에이전트입니다.")
            .isActive(true)
            .description("TripAgent의 SYSTEM 프롬프트")
            .build();
    setId(template, 2L);
    return template;
  }

  public static AiPromptTemplate createTripAgentUserPrompt() {
    AiPromptTemplate template =
        AiPromptTemplate.builder()
            .promptType(PromptType.USER)
            .agentType(AgentType.TRIP_AGENT)
            .prompt("다음 조건으로 여행 일정을 생성해주세요: {region}, {days}일")
            .isActive(true)
            .description("TripAgent의 USER 프롬프트")
            .build();
    setId(template, 3L);
    return template;
  }

  public static AiPromptTemplate createPlaceSummaryAgentSystemPrompt() {
    AiPromptTemplate template =
        AiPromptTemplate.builder()
            .promptType(PromptType.SYSTEM)
            .agentType(AgentType.PLACE_SUMMARY_AGENT)
            .prompt("당신은 여행지 정보를 자연스러운 문단으로 작성하는 전문가입니다.")
            .isActive(true)
            .description("PlaceSummaryAgent의 SYSTEM 프롬프트")
            .build();
    setId(template, 4L);
    return template;
  }

  public static AiPromptTemplate createInactivePrompt() {
    AiPromptTemplate template =
        AiPromptTemplate.builder()
            .promptType(PromptType.SYSTEM)
            .agentType(AgentType.CHAT_AGENT)
            .prompt("비활성화된 프롬프트")
            .isActive(false)
            .description("비활성화된 프롬프트")
            .build();
    setId(template, 5L);
    return template;
  }

  private static void setId(AiPromptTemplate template, Long id) {
    try {
      var idField = template.getClass().getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(template, id);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set ID", e);
    }
  }

  public static AiPromptTemplate createPromptWithoutDescription() {
    return AiPromptTemplate.builder()
        .promptType(PromptType.USER)
        .agentType(AgentType.CHAT_AGENT)
        .prompt("설명이 없는 프롬프트")
        .isActive(false)
        .build();
  }

  public static CreateAiPromptTemplateRequest createPromptTemplateRequest() {
    return CreateAiPromptTemplateRequest.builder()
        .promptType(PromptType.SYSTEM)
        .agentType(AgentType.CHAT_AGENT)
        .prompt("새로운 프롬프트 내용")
        .description("새로운 프롬프트 설명")
        .build();
  }

  public static CreateAiPromptTemplateRequest createPromptTemplateRequestWithoutDescription() {
    return CreateAiPromptTemplateRequest.builder()
        .promptType(PromptType.USER)
        .agentType(AgentType.TRIP_AGENT)
        .prompt("설명 없는 프롬프트")
        .build();
  }

  public static UpdateAiPromptTemplateRequest updatePromptRequest() {
    return UpdateAiPromptTemplateRequest.builder()
        .prompt("수정된 프롬프트 내용")
        .isActive(true)
        .description("수정된 설명")
        .build();
  }

  public static UpdateAiPromptTemplateRequest updatePromptOnlyRequest() {
    return UpdateAiPromptTemplateRequest.builder().prompt("프롬프트만 수정").build();
  }

  public static UpdateAiPromptTemplateRequest updateDescriptionOnlyRequest() {
    return UpdateAiPromptTemplateRequest.builder().description("설명만 수정").build();
  }

  public static UpdateAiPromptTemplateRequest updateIsActiveOnlyRequest(Boolean isActive) {
    return UpdateAiPromptTemplateRequest.builder().isActive(isActive).build();
  }
}
