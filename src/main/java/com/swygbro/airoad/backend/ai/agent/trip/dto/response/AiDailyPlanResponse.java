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
            "해당 일차의 요약 설명. 일정 카테고리별로 소개 내용 작성. 카테고리: 오전 일정, 오후 일정, 저녁 일정. 마크다운 문법 사용. 쌍따옴표(\")는 절대 사용하지 마세요, 작은따옴표(') 사용")
        String description,
    @JsonProperty(required = true, value = "places") @JsonPropertyDescription("방문 장소 배열")
        List<AiScheduledPlaceDto> places,
    @JsonProperty(required = true, value = "nextQuestions")
        @JsonPropertyDescription("유저가 질문하면 좋을 법한 질문 추천 목록")
        List<NextQuestionDto> nextQuestions) {

  /** AI가 생성한 방문 장소 DTO */
  public record AiScheduledPlaceDto(
      @JsonProperty(required = true, value = "placeId")
          @JsonPropertyDescription("DB에 저장된 장소 식별자 ID, null 값을 가질 수 없음")
          Long placeId,
      @JsonProperty(required = true, value = "visitOrder")
          @JsonPropertyDescription("일정 방문 순서 (1부터 시작)")
          int visitOrder,
      @JsonProperty(required = true, value = "category") @JsonPropertyDescription("일정 카테고리")
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
