package com.swygbro.airoad.backend.chat.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메시지 스트림 타입")
public enum MessageStreamType {
  /** 일반 채팅 메세지 */
  @Schema(description = "일반 채팅 메세지")
  CHAT,
  /** 일차별 일정 생성 완료 */
  @Schema(description = "일차별 일정 생성 완료")
  DAILY_PLAN_GENERATED,
  /** 전체 일정 생성 완료 */
  @Schema(description = "전체 일정 생성 완료")
  COMPLETED,
  /** 일정 수정 완료 */
  @Schema(description = "일정 수정 완료")
  UPDATED,
  /** 오류 발생 */
  @Schema(description = "오류 발생")
  ERROR,
  /** 생성 취소됨 */
  @Schema(description = "생성 취소됨")
  CANCELLED,
  /** 일정 수정 시작 */
  @Schema(description = "일정 수정 시작")
  UPDATE_STARTED
}
