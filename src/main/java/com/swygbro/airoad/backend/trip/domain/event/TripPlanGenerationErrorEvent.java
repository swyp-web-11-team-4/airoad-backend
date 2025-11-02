package com.swygbro.airoad.backend.trip.domain.event;

import com.swygbro.airoad.backend.common.exception.ErrorCode;

import lombok.Builder;

/**
 * 여행 일정 생성 중 오류가 발생했을 때 발행되는 이벤트입니다.
 *
 * <p>AI 스트리밍 실패, 파싱 오류, 타임아웃 등의 오류 발생 시 이 이벤트가 발행됩니다.
 *
 * @param chatRoomId 채팅방 ID
 * @param tripPlanId 여행 일정 ID (생성되었을 경우)
 * @param username 사용자 이름 (이메일)
 * @param errorCode 에러 코드
 */
@Builder
public record TripPlanGenerationErrorEvent(
    Long chatRoomId, Long tripPlanId, String username, ErrorCode errorCode) {}
