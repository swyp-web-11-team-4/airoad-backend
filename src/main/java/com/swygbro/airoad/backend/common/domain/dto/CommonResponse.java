package com.swygbro.airoad.backend.common.domain.dto;

import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CommonResponse", description = "공통 API 응답")
public record CommonResponse<T>(
    @Schema(description = "성공 여부", example = "true") boolean success,
    @Schema(description = "HTTP 상태 코드", example = "200") int status,
    @Schema(description = "응답 데이터") T data) {

  /**
   * 성공 응답을 생성합니다.
   *
   * @param data 응답 데이터
   * @param status HTTP 상태 코드
   * @param <T> 데이터 타입
   * @return 성공 ApiResponse 객체
   */
  public static <T> CommonResponse<T> success(int status, T data) {
    return new CommonResponse<>(true, status, data);
  }

  /**
   * 성공 응답을 생성합니다.
   *
   * @param data 응답 데이터
   * @param status HTTP 상태 코드
   * @param <T> 데이터 타입
   * @return 성공 ApiResponse 객체
   */
  public static <T> CommonResponse<T> success(HttpStatus status, T data) {
    return new CommonResponse<>(true, status.value(), data);
  }

  /**
   * 에러 응답을 생성합니다.
   *
   * @param status HTTP 상태 코드
   * @param data 응답 데이터
   * @param <T> 데이터 타입
   * @return 에러 ApiResponse 객체
   */
  public static <T> CommonResponse<T> error(int status, T data) {
    return new CommonResponse<>(false, status, data);
  }

  /**
   * 에러 응답을 생성합니다.
   *
   * @param status HTTP 상태 코드
   * @param data 응답 데이터
   * @param <T> 데이터 타입
   * @return 에러 ApiResponse 객체
   */
  public static <T> CommonResponse<T> error(HttpStatus status, T data) {
    return new CommonResponse<>(false, status.value(), data);
  }
}
