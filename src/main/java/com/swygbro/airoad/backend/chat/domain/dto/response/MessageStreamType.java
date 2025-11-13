package com.swygbro.airoad.backend.chat.domain.dto.response;

public enum MessageStreamType {
  /** 일반 채팅 메세지 */
  CHAT,
  /** 일차별 일정 생성 완료 */
  DAILY_PLAN_GENERATED,
  /** 전체 일정 생성 완료 */
  COMPLETED,
  /** 오류 발생 */
  ERROR,
  /** 생성 취소됨 */
  CANCELLED
}
