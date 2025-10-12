package com.swygbro.airoad.backend.example.domain.dto;

import com.swygbro.airoad.backend.example.domain.entity.Example;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "예제 응답 DTO")
public record ExampleResponse(
    @Schema(description = "Example ID", example = "1") Long id,
    @Schema(description = "Example 이름", example = "Hello, World!") String name) {

  /**
   * Example 엔티티로부터 ExampleResponse를 생성합니다.
   *
   * @param example Example 엔티티
   * @return ExampleResponse
   */
  public static ExampleResponse from(Example example) {
    return new ExampleResponse(example.getId(), example.getName());
  }

  /**
   * 메시지만 포함하는 ExampleResponse를 생성합니다.
   *
   * @param message 메시지
   * @return ExampleResponse
   */
  public static ExampleResponse of(String message) {
    return new ExampleResponse(null, message);
  }
}
