package com.swygbro.airoad.backend.trip.domain.dto;

import java.time.LocalDate;
import java.util.List;

import com.swygbro.airoad.backend.trip.domain.entity.Transportation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 여행 일정 생성 요청 정보를 전달하는 DTO입니다.
 *
 * <p>AI가 여행 일정을 생성하기 위해 필요한 사용자의 선호도 및 여행 조건을 담습니다.
 *
 * @param themes 여행 테마 목록 (예: ["힐링", "맛집", "액티비티"])
 * @param startDate 여행 시작 날짜
 * @param endDate 여행 종료 날짜
 * @param region 선호 지역 (예: "제주", "서울", "부산")
 * @param budget 예산 수준 (예: "저렴", "보통", "고급")
 * @param peopleCount 여행 인원 수
 * @param transportation 선호 이동 수단
 * @param startLocationName 출발지 이름 (예: "서울역")
 * @param startLocationAddress 출발지 주소
 * @param startLocationLat 출발지 위도
 * @param startLocationLng 출발지 경도
 * @param endLocationName 도착지 이름 (예: "제주공항")
 * @param endLocationAddress 도착지 주소
 * @param endLocationLat 도착지 위도
 * @param endLocationLng 도착지 경도
 */
@Builder
@Schema(description = "여행 일정 생성 요청")
public record TripGenerationRequest(
    @Schema(description = "여행 테마 목록", example = "[\"힐링\", \"맛집\"]") List<String> themes,
    @Schema(description = "여행 시작 날짜", example = "2024-03-01") LocalDate startDate,
    @Schema(description = "여행 종료 날짜", example = "2024-03-04") LocalDate endDate,
    @Schema(description = "선호 지역", example = "제주") String region,
    @Schema(description = "예산 수준", example = "보통") String budget,
    @Schema(description = "여행 인원 수", example = "2") Integer peopleCount,
    @Schema(description = "선호 이동 수단", example = "CAR") Transportation transportation,
    @Schema(description = "출발지 이름", example = "서울역") String startLocationName,
    @Schema(description = "출발지 주소", example = "서울특별시 용산구 한강대로 405") String startLocationAddress,
    @Schema(description = "출발지 위도", example = "37.5547125") Double startLocationLat,
    @Schema(description = "출발지 경도", example = "126.9707878") Double startLocationLng,
    @Schema(description = "도착지 이름", example = "제주국제공항") String endLocationName,
    @Schema(description = "도착지 주소", example = "제주특별자치도 제주시 공항로 2") String endLocationAddress,
    @Schema(description = "도착지 위도", example = "33.5113") Double endLocationLat,
    @Schema(description = "도착지 경도", example = "126.4930") Double endLocationLng) {}
