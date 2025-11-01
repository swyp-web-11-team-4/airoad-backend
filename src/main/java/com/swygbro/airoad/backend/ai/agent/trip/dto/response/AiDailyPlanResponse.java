package com.swygbro.airoad.backend.ai.agent.trip.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;

/** AI가 생성한 일별 계획 DTO */
public record AiDailyPlanResponse(
    @JsonProperty(required = true, value = "dayNumber") int dayNumber,
    @JsonProperty(required = true, value = "date") LocalDate date,
    @JsonProperty(required = true, value = "title") String title,
    @JsonProperty(required = true, value = "description") String description,
    @JsonProperty(required = true, value = "places") List<AiScheduledPlaceDto> places,
    @JsonProperty(required = true, value = "nextQuestions") List<NextQuestionDto> nextQuestions) {

  /** AI가 생성한 방문 장소 DTO */
  public record AiScheduledPlaceDto(
      @JsonProperty(required = true, value = "placeId") Long placeId,
      @JsonProperty(required = true, value = "visitOrder") int visitOrder,
      @JsonProperty(required = true, value = "category") ScheduledCategory category,
      @JsonProperty(required = true, value = "startTime") LocalTime startTime,
      @JsonProperty(required = true, value = "endTime") LocalTime endTime,
      @JsonProperty(required = true, value = "travelTime") int travelTime,
      @JsonProperty(required = true, value = "transportation") Transportation transportation) {}

  /** AI가 생성한 다음 추천 질문 */
  public record NextQuestionDto(
      @JsonProperty(required = true, value = "recommendedQuestion") String recommendedQuestion) {}
}
