package com.swygbro.airoad.backend.ai.agent.trip.v3.step;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.KeywordResponse;
import com.swygbro.airoad.backend.ai.agent.trip.v3.context.ExecutionContext;
import com.swygbro.airoad.backend.ai.agent.trip.v3.context.TripPlanContextKey;
import com.swygbro.airoad.backend.ai.agent.trip.v3.pipeline.PipelineStep;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KeywordGenerationStep implements PipelineStep {

  private final ChatClient chatClient;

  private final PromptTemplate keywordPromptTemplate =
      new PromptTemplate(
          """
              당신은 여행 장소 검색 전문가입니다.
              임베딩된 Vector DB에서 검색 키워드 생성을 위해 다음의 2단계 프로세스를 따라 검색 키워드를 생성하세요.

              ## 입력 정보
              - 여행 지역: {region}
              - 테마: {themes}
              - 인원: {peopleCount}
              - 이미 방문한 지역: {visitedDistricts}

              ## 프로세스

              ### 1단계: 행정구역 선택 (1일 1구역 전략)
              입력된 지역({region})에서 테마({themes})와 가장 잘 맞는 행정구역(구, 군) **1곳**을 선택하세요.
              - **중요**: {visitedDistricts}에 포함된 지역은 절대 선택하지 마세요 (다양성 보장)
              - **인접성 우선**: 이전에 방문한 지역이 있다면 그 지역과 **경계를 맞닿은 인접 지역**을 우선 선택하세요
              - **동선 효율화**: 여행객이 일일이 먼 곳으로 이동하지 않도록, 이전 방문 지역과 가깝고 접근성이 좋은 지역을 우선하세요
              - 선택 시 고려사항: 이전 방문 지역과의 근접성(최우선), 테마 적합성, 여행 명소 밀집도, 여행 인원, 하루 일정으로 구성 가능한 범위

              ### 2단계: 키워드 생성
              선택한 행정구역에만 집중하여, 다양한 관점의 검색 키워드를 생성하세요.
              - **장소 관련 키워드 (3~5개)**: "[테마 및 인원에 어울리는 특징]" 형태
                 - 예: "쇼핑몰 혼자 둘러보기", "데이트 할 만한 힐링 장소", "가족 여행지"
              - **음식점 관련 키워드 (3~5개)**: "[음식점 유형] + [음식 특징]" 형태
                 - 예: "프리미엄 한정식", "현지인 맛집", "혼밥하기 좋은 음식점"

              ## 필수 제약 사항
              1. **행정구역 단일화**: 모든 키워드는 선택된 1개 행정구역 내에서만 생성
                 - 절대로 여러 지역을 섞어서 생성하지 마세요
              2. **카페 제외**: 음식점 키워드에서 카페는 절대 포함하지 않음
              3. **한글 사용**: 반드시 한글로 키워드 생성
              """);

  public KeywordGenerationStep(@Qualifier("openAiChatModel") ChatModel chatModel) {
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultAdvisors(
                StructuredOutputValidationAdvisor.builder()
                    .outputType(KeywordResponse.class)
                    .maxRepeatAttempts(3)
                    .build())
            .build();
  }

  @Override
  public ExecutionContext execute(ExecutionContext context) {
    AiDailyPlanRequest request = context.get(TripPlanContextKey.REQUEST);
    List<String> visitedDistricts = context.get(TripPlanContextKey.VISITED_DISTRICTS);

    String themes =
        request.themes().stream()
            .map(PlaceThemeType::getDescription)
            .collect(Collectors.joining(", "));

    String districts =
        visitedDistricts == null || visitedDistricts.isEmpty()
            ? "없음"
            : String.join(", ", visitedDistricts);

    String prompt =
        keywordPromptTemplate.render(
            Map.of(
                "region",
                request.region(),
                "themes",
                themes,
                "peopleCount",
                request.peopleCount(),
                "visitedDistricts",
                districts));

    ChatClientRequestSpec chatClientRequest = chatClient.prompt().system(prompt);

    if (request.userMessage() != null && !request.userMessage().isBlank()) {
      chatClientRequest = chatClientRequest.user(request.userMessage());
    }

    KeywordResponse response = chatClientRequest.call().entity(KeywordResponse.class);

    log.info("생성된 검색 키워드: {}", response);
    log.info(
        "선택된 행정구역: {} {}",
        Objects.requireNonNull(response).selectedProvince(),
        response.selectedDistrict());

    context.put(TripPlanContextKey.KEYWORDS, response);
    context.put(TripPlanContextKey.SELECTED_DISTRICT, response.selectedDistrict());
    context.put(TripPlanContextKey.SELECTED_PROVINCE, response.selectedProvince());

    return context;
  }

  @Override
  public String getName() {
    return "KeywordGeneration";
  }
}
