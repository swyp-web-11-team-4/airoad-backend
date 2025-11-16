package com.swygbro.airoad.backend.ai.application.context.dto;

import java.time.LocalDate;
import java.util.List;

import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;

import lombok.Builder;

/**
 * 여행 계획 생성을 위한 Command 컨텍스트
 *
 * <p>TripPlanCommandContextProvider에서 사용하는 데이터를 담습니다.
 *
 * <p>CQRS 패턴의 Command 측면을 담당하며, 새로운 여행 계획을 생성하기 위한 입력 데이터를 제공합니다.
 *
 * @param region 여행 지역
 * @param startDate 시작 날짜
 * @param duration 여행 기간 (일)
 * @param themes 여행 테마 목록
 * @param peopleCount 여행 인원 수
 * @param transportation 이동 수단
 */
@Builder
public record TripPlanCommandContext(
    String region,
    LocalDate startDate,
    Integer duration,
    List<PlaceThemeType> themes,
    Integer peopleCount,
    Transportation transportation) {}
