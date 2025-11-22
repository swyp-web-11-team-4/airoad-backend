package com.swygbro.airoad.backend.ai.agent.trip.v2.worker;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.common.optimizer.Optimizer.OptimizationPlan;
import com.swygbro.airoad.backend.ai.agent.common.worker.Worker;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.KeywordResponse;
import com.swygbro.airoad.backend.ai.agent.trip.v2.context.TripPlanContext;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class KeywordWorker implements Worker<TripPlanContext, TripPlanTaskType> {

  private final ChatClient chatClient;

  public KeywordWorker(@Qualifier("openAiChatModel") ChatModel chatModel) {
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
  public TripPlanTaskType getTaskType() {
    return TripPlanTaskType.KEYWORD;
  }

  private final PromptTemplate keywordPromptTemplate =
      new PromptTemplate(
          """
      당신은 여행 장소 검색 전문가입니다.
      다음의 2단계 프로세스를 따라 검색 키워드를 생성하세요.

      ## 입력 정보
      - 여행 지역: {region}
      - 사용자 테마: {themes}

      ## 피드백 및 추가 요청사항
      {feedback}

      ## 프로세스

      ### 1단계: 서브 지역 선택
      입력된 지역({region})에서 사용자 테마({themes})와 가장 잘 맞는 특정 서브 지역 **1개**를 선택하세요.
      - 예시: "서울" → "강남", "강북", "종로" 중 1개 선택
      - 예시: "경기도" → "수원", "성남", "용인" 중 1개 선택
      - 선택 시 고려사항: 테마 적합성, 여행 명소 밀집도, 하루 일정으로 구성 가능한 범위

      ### 2단계: 키워드 생성
      선택한 서브 지역에 집중하여, 다양한 관점의 검색 키워드를 생성하세요.
      - **장소 관련 키워드 (3~5개)**: "[서브지역] + [테마 관련 구체적 장소]" 형태
         - 예: "강남 트렌디한 갤러리", "강남 한강공원 인근 경관지", "강남 조용한 독서실"
      - **음식점 관련 키워드 (3~5개)**: "[서브지역] + [음식 특징]" 형태
         - 예: "강남 프리미엄 한정식", "강남 현지인 맛집", "강남 건강식 카페"

      ## 필수 제약 사항
      1. **지역 통일성**: 모든 키워드는 선택된 서브 지역 내에서만 생성하세요.
         - 절대로 여러 지역을 섞어서 생성하지 마세요.
      2. **피드백 반영**:
         - 피드백이 없다면(최초 실행) 선택 서브 지역 내에서 다양한 관점의 키워드를 생성하세요.
         - 피드백이 있다면:
           * 구체적인 주소/지역명(예: "용인시 처인구", "강남역 주변")이 포함되어 있으면, 그 지역에 집중하여 키워드를 생성하세요.
           * 예: 피드백에 "용인시 처인구 근처"라 하면, 처인구에 특화된 키워드만 생성
           * 예: 피드백에 "강남역 주변"이라 하면, 강남역 근처의 키워드만 생성
           * 필요시 기존 서브 지역과 다른 지역으로 변경할 수 있습니다.
      3. **카페 제외**: 음식점 키워드에서 카페는 절대 포함하지 마세요.
      """);

  @Override
  public void execute(TripPlanContext context) {
    String themes =
        context.getRequest().themes().stream()
            .map(PlaceThemeType::getDescription)
            .collect(Collectors.joining(", "));

    String feedback = "없음 (최초 생성 단계)";
    OptimizationPlan plan = context.getOptimizationPlan();

    if (plan != null && plan.recommendation() != null) {
      feedback = plan.recommendation();
    }

    log.info("KeywordWorker 실행 - 피드백 반영 여부: {}", !feedback.startsWith("없음"));

    String prompt =
        keywordPromptTemplate.render(
            Map.of(
                "region", context.getRequest().region(),
                "themes", themes,
                "feedback", feedback));

    KeywordResponse response =
        chatClient.prompt().user(prompt).call().entity(KeywordResponse.class);

    log.info("생성된 검색 키워드: {}", response);
    context.setKeywords(Objects.requireNonNull(response));
  }
}
