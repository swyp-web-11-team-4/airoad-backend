package com.swygbro.airoad.backend.ai.agent.trip.v3.step;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.v3.context.ExecutionContext;
import com.swygbro.airoad.backend.ai.agent.trip.v3.context.TripPlanContextKey;
import com.swygbro.airoad.backend.ai.agent.trip.v3.dto.DeduplicationResponse;
import com.swygbro.airoad.backend.ai.agent.trip.v3.dto.ScheduleCreationResponse;
import com.swygbro.airoad.backend.ai.agent.trip.v3.pipeline.PipelineStep;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ScheduleCreationStep implements PipelineStep {

  private final ChatClient chatClient;

  public ScheduleCreationStep(@Qualifier("openAiChatModel") ChatModel chatModel) {
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultAdvisors(
                StructuredOutputValidationAdvisor.builder()
                    .outputType(ScheduleCreationResponse.class)
                    .maxRepeatAttempts(3)
                    .build())
            .build();
  }

  private final PromptTemplate schedulePromptTemplate =
      new PromptTemplate(
          """
      당신은 전문 여행 플래너입니다.
      후보 장소 목록에서 가장 적절한 장소들을 선택하여 효율적이고 즐거운 {dayNumber}일차 여행 일정을 계획하세요.

      ## 후보 목록
      ### 장소 관련 목록
      {places}

      ### 음식점 관련 목록
      {restaurants}

      ## 필수 제약 사항
      1. **절대 거짓 정보를 생성하지 마십시오.**
         - 반드시 위 `후보 목록`에 있는 장소의 `ID` 정보만 사용해야 합니다.
         - 목록에 없는 장소를 절대 사용하지 마세요.
      2. **일정 구성 로직**:
         - `오전 일정 -> 점심 식사 -> 오후 일정 -> 저녁 식사 -> 저녁 일정`의 흐름을 기본으로 하세요.
           - 1. MORNING (오전 일정)
           - 2. LUNCH (점심 식사 - 반드시 음식점)
           - 3. AFTERNOON (오후 일정)
           - 4. DINNER (저녁 식사 - 반드시 음식점)
           - 5. EVENING (저녁 일정)
         - 총 5개의 스팟을 배치하세요.
      3. **동선 최적화**:
         - 제공된 `주소(Address)`를 참고하여 서로 비슷한 주소끼리 묶으세요.
         - 동선이 꼬이지 않도록 지리적으로 인접한 순서대로 방문하게 하세요.
      4. **식사 장소**:
         - **점심(LUNCH)과 저녁(DINNER)은 서로 다른 음식 종류를 선택해야 합니다.**
         - 같은 식당이나 같은 음식 카테고리를 중복 선택하는 것은 절대 금지입니다.
         - 카페는 선택하지 마세요. 반드시 식사에 적합한 음식점을 선택하세요.
     """);

  @Override
  public ExecutionContext execute(ExecutionContext context) {
    DeduplicationResponse input = context.get(TripPlanContextKey.DEDUP_RESULT);
    AiDailyPlanRequest request = context.get(TripPlanContextKey.REQUEST);
    Integer dayNumber = context.get(TripPlanContextKey.DAY_NUMBER);

    List<Document> places = input.places();
    List<Document> restaurants = input.restaurants();

    if (places == null || places.isEmpty()) {
      log.warn("추천 장소가 없음");
      return context;
    }

    String placesText = convertDocumentToString(places);
    String restaurantsText = convertDocumentToString(restaurants);

    String prompt =
        schedulePromptTemplate.render(
            Map.of(
                "places", placesText,
                "restaurants", restaurantsText,
                "dayNumber", dayNumber.toString()));

    ChatClientRequestSpec chatClientRequest = chatClient.prompt().system(prompt);

    if (request.userMessage() != null && !request.userMessage().isBlank()) {
      chatClientRequest = chatClientRequest.user(request.userMessage());
    }

    ScheduleCreationResponse scheduleResult =
        chatClientRequest.call().entity(ScheduleCreationResponse.class);

    log.info("일정 선택 완료: {}개 장소 포함", scheduleResult.places().size());

    context.put(TripPlanContextKey.SCHEDULE_CREATION_RESULT, scheduleResult);

    return context;
  }

  @Override
  public String getName() {
    return "ScheduleCreation";
  }

  private String convertDocumentToString(List<Document> documents) {
    return documents.stream()
        .map(this::formatDocumentWithMetadata)
        .collect(Collectors.joining("\n"));
  }

  private String formatDocumentWithMetadata(Document doc) {
    Map<String, Object> metadata = doc.getMetadata();
    return String.format(
        """
        - [ID:%s] %s
          테마: %s
          주소: %s
          설명: %s
        """,
        metadata.get("placeId"),
        metadata.get("name"),
        metadata.get("themes"),
        metadata.get("address"),
        doc.getText());
  }
}
