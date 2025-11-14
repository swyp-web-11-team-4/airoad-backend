package com.swygbro.airoad.backend.ai.agent.trip.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;

/** AI가 생성한 일별 계획 DTO */
public record AiDailyPlanResponse(
    @JsonProperty(required = true, value = "dayNumber")
        @JsonPropertyDescription("1부터 N까지 순차적으로 증가하는 일차 번호")
        int dayNumber,
    @JsonProperty(required = true, value = "date") @JsonPropertyDescription("'yyyy-MM-dd' 형식의 날짜")
        LocalDate date,
    @JsonProperty(required = true, value = "title") @JsonPropertyDescription("해당 일차의 소제목")
        String title,
    @JsonProperty(required = true, value = "description")
        @JsonPropertyDescription(
            """
            해당 일차의 요약 설명입니다. 마크다운 형식으로 작성하되 다음 규칙을 준수하세요:
            1. 쌍따옴표(") 사용 금지, 작은따옴표(') 사용
            2. 아래 형식으로 작성:

            **n일차 - {제목}**
            - **{일정 카테고리}**: {장소}
              {설명}
            - **{일정 카테고리}**: {장소}
              {설명}
            - **{일정 카테고리}**: {장소}
              {설명}

            {일정 요약}: Optional

            예시:
            **1일차 - 제주 동부 해안 탐방**
            - **오전 일정**: 성산일출봉
              제주를 대표하는 일출 명소로, 유네스코 세계자연유산입니다. 정상까지 약 30분 소요됩니다.
            - **오후 일정**: 섭지코지
              드라마 촬영지로 유명한 아름다운 해안가입니다. 카페에서 바다를 바라보며 휴식을 취할 수 있습니다.
            - **저녁 일정**: 성산 해녀의집
              신선한 해산물 정식을 맛볼 수 있는 현지 맛집입니다.

            제주의 동부 해안을 따라 자연 경관을 감상하며 여유로운 하루를 보냅니다.
            """)
        String description,
    @JsonProperty(required = true, value = "places") @JsonPropertyDescription("방문 장소 배열")
        List<AiScheduledPlaceDto> places) {

  /** AI가 생성한 방문 장소 DTO */
  public record AiScheduledPlaceDto(
      @JsonProperty(required = true, value = "placeId")
          @JsonPropertyDescription("DB에 저장된 장소 식별자 ID, null 값을 가질 수 없음")
          Long placeId,
      @JsonProperty(required = true, value = "visitOrder")
          @JsonPropertyDescription("일정 방문 순서 (1부터 시작)")
          int visitOrder,
      @JsonProperty(required = true, value = "category")
          @JsonPropertyDescription(
              "일정 카테고리. 반드시 다음 중 하나만 사용: MORNING(오전 일정, 아침~점심 전), AFTERNOON(오후 일정, 점심~저녁 전), EVENING(저녁 일정, 저녁~밤). 점심 식사 - `AFTERNOON`, 저녁 식사 - `EVENING` 으로 분류")
          ScheduledCategory category,
      @JsonProperty(required = true, value = "startTime")
          @JsonPropertyDescription("일정 시작 시간. 'HH:mm' 형식 (예: 09:00, 13:30)")
          LocalTime startTime,
      @JsonProperty(required = true, value = "endTime")
          @JsonPropertyDescription("일정 종료 시간. 'HH:mm' 형식 (예: 11:00, 15:30)")
          LocalTime endTime,
      @JsonProperty(required = true, value = "travelTime")
          @JsonPropertyDescription("이전 장소로부터의 이동 시간(분)")
          int travelTime,
      @JsonProperty(required = true, value = "transportation") @JsonPropertyDescription("이동 수단")
          Transportation transportation) {}

  /** AI가 생성한 다음 추천 질문 */
  public record NextQuestionDto(
      @JsonProperty(required = true, value = "recommendedQuestion")
          @JsonPropertyDescription("추천하는 질문 내용 (예: 'OOO 운영 시간에 대해 알려주세요', '다른 장소로 일정을 변경하고 싶어요')")
          String recommendedQuestion) {}
}
