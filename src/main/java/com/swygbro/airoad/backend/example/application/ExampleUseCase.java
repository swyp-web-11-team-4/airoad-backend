package com.swygbro.airoad.backend.example.application;

import com.swygbro.airoad.backend.example.domain.dto.ExampleResponse;

public interface ExampleUseCase {
  /**
   * Hello 메시지를 반환합니다.
   *
   * @return ExampleResponse
   */
  ExampleResponse getHelloMessage();

  /**
   * ID로 Example을 조회합니다.
   *
   * @param id Example ID
   * @return ExampleResponse
   */
  ExampleResponse getExampleById(Long id);

  /**
   * 새로운 Example을 생성합니다.
   *
   * @param name Example 이름
   * @return ExampleResponse
   */
  ExampleResponse createExample(String name);
}
