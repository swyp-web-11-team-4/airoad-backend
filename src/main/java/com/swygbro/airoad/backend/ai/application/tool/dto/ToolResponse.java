package com.swygbro.airoad.backend.ai.application.tool.dto;

/**
 * AI Tool 실행 결과의 표준 응답 형식
 *
 * <p>모든 Tool은 이 타입을 반환하여 일관성을 보장합니다.
 *
 * @param success 작업 성공 여부
 * @param message AI 모델에게 전달할 메시지
 */
public record ToolResponse(boolean success, String message) {

  /**
   * 성공 응답 생성
   *
   * @param message AI 모델에게 전달할 성공 메시지
   * @return 성공 ToolResponse
   */
  public static ToolResponse success(String message) {
    return new ToolResponse(true, message);
  }

  /**
   * 실패 응답 생성
   *
   * @param message AI 모델에게 전달할 에러 메시지
   * @return 실패 ToolResponse
   */
  public static ToolResponse failure(String message) {
    return new ToolResponse(false, message);
  }
}
