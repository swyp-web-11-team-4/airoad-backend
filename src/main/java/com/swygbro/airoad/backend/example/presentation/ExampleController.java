package com.swygbro.airoad.backend.example.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.example.application.ExampleUseCase;
import com.swygbro.airoad.backend.example.domain.dto.ExampleResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Example", description = "Example API 예제")
@RestController
@RequestMapping("/api/v1/examples")
@RequiredArgsConstructor
public class ExampleController {

  private final ExampleUseCase exampleUseCase;

  @Operation(summary = "Example 조회", description = "ID로 Example을 조회합니다.")
  @GetMapping("/{id}")
  public ResponseEntity<CommonResponse<ExampleResponse>> getExample(
      @Parameter(description = "Example ID", example = "1") @PathVariable Long id) {
    ExampleResponse data = exampleUseCase.getExampleById(id);
    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, data));
  }

  @Operation(summary = "Example 생성", description = "새로운 Example을 생성합니다.")
  @PostMapping
  public ResponseEntity<CommonResponse<ExampleResponse>> createExample(
      @Parameter(description = "Example 이름", example = "Test Example") @RequestParam String name) {
    ExampleResponse data = exampleUseCase.createExample(name);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(CommonResponse.success(HttpStatus.CREATED, data));
  }

  @Operation(summary = "Example 삭제", description = "Example을 삭제합니다.")
  @DeleteMapping("/{id}")
  public ResponseEntity<CommonResponse<Void>> deleteExample(
      @Parameter(description = "Example ID") @PathVariable Long id) {
    exampleUseCase.deleteExample(id);
    return ResponseEntity.status(HttpStatus.NO_CONTENT)
        .body(CommonResponse.success(HttpStatus.NO_CONTENT, null));
  }
}
