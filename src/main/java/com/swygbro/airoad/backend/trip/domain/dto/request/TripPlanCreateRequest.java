package com.swygbro.airoad.backend.trip.domain.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Builder;

/**
 * 여행 일정 생성 요청 정보를 전달하는 DTO입니다.
 *
 * <p>여행 일정을 생성하기 위해 필요한 사용자의 선호도 및 여행 조건을 담습니다.
 *
 * @param themes 여행 테마 목록 (예: ["힐링", "맛집", "액티비티"])
 * @param startDate 여행 시작 날짜
 * @param duration 여행 기간 (일)
 * @param region 선호 지역 (예: "제주", "서울", "부산")
 * @param peopleCount 여행 인원 수
 */
@Builder
public record TripPlanCreateRequest(
    @NotEmpty(message = "여행 테마는 최소 1개 이상 선택해야 합니다.") List<String> themes,
    @NotNull(message = "여행 시작 날짜는 필수입니다.") LocalDate startDate,
    @NotNull(message = "여행 기간은 필수입니다.") @Min(value = 1, message = "여행 기간은 최소 1일 이상이어야 합니다.")
        Integer duration,
    @NotBlank(message = "여행 지역은 필수입니다.") String region,
    @NotNull(message = "여행 인원은 필수입니다.") @Min(value = 1, message = "여행 인원은 최소 1명 이상이어야 합니다.")
        Integer peopleCount) {}
