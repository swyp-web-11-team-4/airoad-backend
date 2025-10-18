package com.swygbro.airoad.backend.realtime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 여행 일정 응답 DTO (테스트용 더미)
 *
 * <p>AI로부터 받아오는 여행 일정을 실시간으로 전송하기 위한 DTO입니다.
 *
 * <p><strong>구독 경로</strong>: {@code /user/sub/schedule}
 *
 * @param scheduleId 일정 ID
 * @param tripName 여행 이름
 * @param date 일정 날짜
 * @param activities 활동 목록
 * @param generatedAt 생성 시각
 */
public record ScheduleResponse(
    Long scheduleId,
    String tripName,
    LocalDate date,
    List<String> activities,
    LocalDateTime generatedAt) {}
