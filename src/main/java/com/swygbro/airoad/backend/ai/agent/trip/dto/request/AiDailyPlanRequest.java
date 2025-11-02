package com.swygbro.airoad.backend.ai.agent.trip.dto.request;

import java.time.LocalDate;
import java.util.List;

import com.swygbro.airoad.backend.trip.domain.entity.Transportation;

import lombok.Builder;

/**
 * 여행 일정 생성 요청 정보를 전달하는 DTO입니다.
 *
 * <p>AI가 여행 일정을 생성하기 위해 필요한 사용자의 선호도 및 여행 조건을 담습니다.
 *
 * @param chatRoomId 채팅방 ID
 * @param tripPlanId 여행 계획 ID
 * @param username 사용자 이름 (이메일)
 * @param themes 여행 테마 목록 (예: ["힐링", "맛집", "액티비티"])
 * @param startDate 여행 시작 날짜
 * @param duration 여행 기간 (일)
 * @param region 선호 지역 (예: "제주", "서울", "부산")
 * @param peopleCount 여행 인원 수
 * @param transportation 선호 이동 수단
 */
@Builder
public record AiDailyPlanRequest(
    Long chatRoomId,
    Long tripPlanId,
    String username,
    List<String> themes,
    LocalDate startDate,
    Integer duration,
    String region,
    Integer peopleCount,
    Transportation transportation) {}
