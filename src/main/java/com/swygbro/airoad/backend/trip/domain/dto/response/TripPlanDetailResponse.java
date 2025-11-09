package com.swygbro.airoad.backend.trip.domain.dto.response;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 여행 일정 상세 조회 응답 DTO
 *
 * <p>여행 일정의 상세 정보를 제공합니다.
 *
 * @param tripPlanId 여행 계획 ID
 * @param title 여행 제목
 * @param region 여행 지역
 * @param startDate 여행 시작일
 * @param duration 여행 기간 (일)
 * @param peopleCount 여행 인원 수
 * @param themes 여행 테마 목록
 */
@Builder
@Schema(description = "여행 일정 상세 조회 응답")
public record TripPlanDetailResponse(
    @Schema(description = "여행 계획 ID", example = "123") Long tripPlanId,
    @Schema(description = "여행 제목", example = "제주도 힐링 여행") String title,
    @Schema(description = "여행 지역", example = "제주") String region,
    @Schema(description = "여행 시작일", example = "2025-03-01") LocalDate startDate,
    @Schema(description = "여행 기간 (일)", example = "3") Integer duration,
    @Schema(description = "여행 인원 수", example = "2") Integer peopleCount,
    @Schema(description = "여행 테마 목록", example = "[\"HEALING\", \"FAMOUS_SPOT\"]")
        List<PlaceThemeType> themes) {

  /**
   * TripPlan 엔티티로부터 TripPlanDetailResponse를 생성합니다.
   *
   * @param tripPlan 여행 계획 엔티티
   * @return 여행 계획 상세 응답 DTO
   */
  public static TripPlanDetailResponse from(TripPlan tripPlan) {
    int duration =
        (int) ChronoUnit.DAYS.between(tripPlan.getStartDate(), tripPlan.getEndDate()) + 1;

    return TripPlanDetailResponse.builder()
        .tripPlanId(tripPlan.getId())
        .title(tripPlan.getTitle())
        .region(tripPlan.getRegion())
        .startDate(tripPlan.getStartDate())
        .duration(duration)
        .peopleCount(tripPlan.getPeopleCount())
        .themes(tripPlan.getTripThemes())
        .build();
  }
}
