package com.swygbro.airoad.backend.ai.agent.trip.step;

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

import com.swygbro.airoad.backend.ai.agent.trip.context.ExecutionContext;
import com.swygbro.airoad.backend.ai.agent.trip.context.TripPlanContextKey;
import com.swygbro.airoad.backend.ai.agent.trip.dto.request.AiDailyPlanRequest;
import com.swygbro.airoad.backend.ai.agent.trip.dto.response.KeywordResponse;
import com.swygbro.airoad.backend.ai.agent.trip.pipeline.PipelineStep;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KeywordGenerationStep implements PipelineStep {

  private final ChatClient chatClient;

  private final PromptTemplate keywordPromptTemplate =
      new PromptTemplate(
          """
              당신은 Vector DB 검색 최적화 전문가입니다.
              DB에 저장된 데이터는 "강원도 삼척시에 위치한 OO은 가족과 함께하기 좋은..." 형태의 서술형 텍스트입니다.

              <입력정보>
              <지역>{region}</지역>
              <테마>{themes}</테마>
              <인원>{peopleCount}명</인원>
              <현재일차>{dayNumber}일차</현재일차>
              <전체일정>{duration}일</전체일정>
              <방문한지역>{visitedDistricts}</방문한지역>
              </입력정보>

              <작업설명>
              위 입력정보를 바탕으로 벡터 유사도가 높은 자연어 검색 키워드를 생성하세요.

              ## 1단계: 행정구역 선택 (시/군/구 단위만 허용)

              필수 규칙:
              - 금지: 동/읍/면 단위 사용 (예: 애월읍 X → 제주시 O)
              - 필수: 공식 명칭 사용 (강원특별자치도, 제주특별자치도, 서울특별시 등)

              일차별 선택 전략:
              - 현재: {dayNumber}일차
              - 전체: {duration}일
              - 마지막 날 여부: {isLastDay}

              ## 최우선 규칙: 마지막 날 체크

              **{isLastDay}가 true인 경우 (마지막 날):**
              → 반드시 공항/주요역/터미널이 있는 행정구역 선택 (출발 복귀)
              → 제주특별자치도 → 제주시(제주국제공항)
              → 강원특별자치도 → 강릉시(강릉역), 춘천시(춘천역)
              → 서울특별시 → 용산구(서울역), 강남구(강남터미널)
              → 부산광역시 → 동래구(부산역)

              **{dayNumber}가 1인 경우 (1일차, 마지막 날 아님):**
              → 위와 동일한 교통 거점 행정구역 선택 (여행 시작점)

              **그 외 (중간일):**

              지역 유형 판단:
              - 광역 (제주/강원/경상남도 등): 시/군이 여러 개, 거리 멀음
              - 광역시/특별시 (서울/부산/대구 등): 구가 여러 개, 거리 가까움

              광역 지역:
              → 반드시 방문한 지역({visitedDistricts})과 다른 시/군 선택

              광역시/특별시:
              → 매일 다른 구 선택 ({visitedDistricts} 제외)
              → 테마에 맞는 구 선택

              ## 2단계: 검색 키워드 생성

              중요: 선택된 행정구역은 DB 필터링에만 사용됩니다.
              벡터 검색의 정확도를 높이려면 키워드에 세부 지역 정보를 포함하세요.

              모든 키워드에 반드시 포함할 요소:

              1. 지역 표현 (2단계 구조):
                 - 선택된 행정구역명 (예: 제주시, 서귀포시, 춘천시)
                 - 세부 지역 (동/읍/면 단위 또는 지역명)
                 - 예: "제주시 애월읍", "서귀포시 중문동", "춘천시 신동면"

              2. 인원 표현 (현재 {peopleCount}명 기준):
                 - 1명: "혼자", "1인", "혼밥", "조용한", "사색하기 좋은"
                 - 2명: "친구", "커플", "2인", "데이트", "오붓한", "둘이서"
                 - 3-4명: "친구들과 함께", "가족", "아이와 함께", "부모님과", "넓은 공간"
                 - 5명 이상: "단체", "모임", "대형 테이블", "회식"

              3. 위치 맥락 (현재 {dayNumber}일차 기준):
                 - 1일차 또는 {duration}일차: 교통거점 세부 위치 (예: "제주국제공항 근처", "강릉역 도보권")
                 - 중간일: 관광지/테마 중심 세부 지역

              4. 활동/특징 서술:
                 - DB 임베딩과 유사한 서술형 표현
                 - 예: "위치한", "즐길 수 있는", "체험할 수 있는"

              출력: 장소 키워드 3-5개 + 음식점 키워드 3-5개
              </작업설명>

              <예시>
              <예시1>
              입력: 제주특별자치도, 2명, 1일차/3일, 액티비티
              선택된 행정구역: 제주시 (제주국제공항 위치)

              장소 키워드:
              - "제주시 용담동 제주국제공항 근처에 위치한 커플이 함께 즐기는 해변 카약 체험"
              - "제주시 도두동 공항 인근에 위치한 2인 스노클링 투어 프로그램"

              음식점 키워드:
              - "제주시 용담동 제주국제공항 근처에 위치한 커플 데이트하기 좋은 고기국수 전문점"
              - "제주시 노형동 공항 도보권에 위치한 2인 세트 메뉴 있는 흑돼지 맛집"
              </예시1>

              <예시2>
              입력: 제주특별자치도, 4명, 2일차/3일, 문화체험, 방문한지역: 제주시
              선택된 행정구역: 서귀포시

              장소 키워드:
              - "서귀포시 중문동에 위치한 가족 단위 아이와 함께 체험하는 전통 공예 프로그램"
              - "서귀포시 성산읍에 위치한 부모님과 둘러보기 좋은 문화유산 탐방 코스"

              음식점 키워드:
              - "서귀포시 서귀동에 위치한 가족 단위 넓은 좌석의 갈치조림 전문점"
              - "서귀포시 중문동에 위치한 아이 메뉴 있는 4인 식사하기 좋은 한정식"
              </예시2>
              </예시>

              <제약사항>
              - 음식점 키워드에 카페, 베이커리, 디저트 절대 제외
              - 자연스러운 명사형 종결: "~하는 곳", "~맛집", "~명소"
              </제약사항>
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
    Integer dayNumber = context.get(TripPlanContextKey.DAY_NUMBER);
    List<String> visitedDistricts = context.get(TripPlanContextKey.VISITED_DISTRICTS);

    String themes =
        request.themes().stream()
            .map(PlaceThemeType::getDescription)
            .collect(Collectors.joining(", "));

    String districts =
        visitedDistricts == null || visitedDistricts.isEmpty()
            ? "없음"
            : String.join(", ", visitedDistricts);

    boolean isLastDay = dayNumber.equals(request.duration());

    String prompt =
        keywordPromptTemplate.render(
            Map.of(
                "region",
                request.region(),
                "themes",
                themes,
                "peopleCount",
                request.peopleCount(),
                "dayNumber",
                dayNumber,
                "duration",
                request.duration(),
                "isLastDay",
                isLastDay ? "true" : "false",
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
        Objects.requireNonNull(response).selectedProvince().getOfficialName(),
        response.selectedDistrict());

    context.put(TripPlanContextKey.KEYWORDS, response);
    context.put(TripPlanContextKey.SELECTED_DISTRICT, response.selectedDistrict());
    context.put(
        TripPlanContextKey.SELECTED_PROVINCE, response.selectedProvince().getOfficialName());

    return context;
  }

  @Override
  public String getName() {
    return "KeywordGeneration";
  }
}
