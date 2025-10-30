package com.swygbro.airoad.backend.trip.domain.dto.request;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/**
 * 여행 일정 생성 요청 정보를 전달하는 DTO입니다.
 *
 * <p>여행 일정을 생성하기 위해 필요한 사용자의 선호도 및 여행 조건을 담습니다.
 *
 * @param themes      여행 테마 목록 (예: ["힐링", "맛집", "액티비티"])
 * @param startDate   여행 시작 날짜
 * @param duration    여행 기간 (일)
 * @param region      선호 지역 (예: "제주", "서울", "부산")
 * @param peopleCount 여행 인원 수
 */
@Builder
public record TripPlanCreateRequest(
    List<String> themes,
    LocalDate startDate,
    Integer duration,
    String region,
    Integer peopleCount) {
}
