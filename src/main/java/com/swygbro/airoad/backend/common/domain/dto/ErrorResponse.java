package com.swygbro.airoad.backend.common.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorResponse", description = "에러 응답")
public record ErrorResponse(
    @Schema(description = "타임스탬프", example = "2025-10-12T00:00:00") LocalDateTime timestamp,
    @Schema(description = "에러 코드", example = "COMMON001") String code,
    @Schema(description = "에러 메시지", example = "잘못된 요청입니다.") String message,
    @Schema(description = "요청 경로", example = "/api/v1/example") String path,
    @Schema(description = "상세 에러 목록") List<FieldError> errors) {

  /**
   * 일반 에러 응답을 생성합니다.
   *
   * @param code 에러 코드
   * @param message 에러 메시지
   * @param path 요청 경로
   * @return ErrorResponse 객체
   */
  public static ErrorResponse of(String code, String message, String path) {
    return new ErrorResponse(LocalDateTime.now(), code, message, path, null);
  }

  /**
   * 필드 에러를 포함한 에러 응답을 생성합니다.
   *
   * @param code 에러 코드
   * @param message 에러 메시지
   * @param path 요청 경로
   * @param errors 필드 에러 목록
   * @return ErrorResponse 객체
   */
  public static ErrorResponse of(
      String code, String message, String path, List<FieldError> errors) {
    return new ErrorResponse(LocalDateTime.now(), code, message, path, errors);
  }

  @Schema(description = "필드 에러 상세")
  public record FieldError(
      @Schema(description = "필드명", example = "email") String field,
      @Schema(description = "거부된 값", example = "invalid-email") Object rejectedValue,
      @Schema(description = "에러 메시지", example = "올바른 이메일 형식이 아닙니다.") String message) {

    /**
     * 필드 에러를 생성합니다.
     *
     * @param field 필드명
     * @param rejectedValue 거부된 값
     * @param message 에러 메시지
     * @return FieldError 객체
     */
    public static FieldError of(String field, Object rejectedValue, String message) {
      return new FieldError(field, rejectedValue, message);
    }
  }
}
