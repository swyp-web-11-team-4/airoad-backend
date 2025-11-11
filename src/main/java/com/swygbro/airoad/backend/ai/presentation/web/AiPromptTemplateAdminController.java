package com.swygbro.airoad.backend.ai.presentation.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swygbro.airoad.backend.ai.application.command.AiPromptTemplateCommandUseCase;
import com.swygbro.airoad.backend.ai.application.query.AiPromptTemplateQueryUseCase;
import com.swygbro.airoad.backend.ai.domain.dto.request.CreateAiPromptTemplateRequest;
import com.swygbro.airoad.backend.ai.domain.dto.request.UpdateAiPromptTemplateRequest;
import com.swygbro.airoad.backend.ai.domain.dto.response.AiPromptTemplateResponse;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.common.domain.dto.PageResponse;

import lombok.RequiredArgsConstructor;

/** AI 프롬프트 템플릿 관리를 위한 Admin API 컨트롤러 */
@RestController
@RequestMapping("/api/v1/admin/prompts")
@RequiredArgsConstructor
@Validated
public class AiPromptTemplateAdminController {

  private final AiPromptTemplateCommandUseCase aiPromptTemplateCommandUseCase;
  private final AiPromptTemplateQueryUseCase aiPromptTemplateQueryUseCase;

  /** AI 프롬프트 템플릿 목록을 페이지네이션하여 조회합니다. */
  @GetMapping
  public ResponseEntity<CommonResponse<PageResponse<AiPromptTemplateResponse>>> getPromptTemplates(
      @RequestParam(defaultValue = "0")
          @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
          @Max(value = 100, message = "페이지 번호는 100 이하여야 합니다.")
          int page,
      @RequestParam(defaultValue = "10") @Min(value = 1, message = "페이지 사이즈는 1 이상이어야 합니다.")
          int size,
      @RequestParam(defaultValue = "createdAt", required = false)
          @Pattern(
              regexp = "^(createdAt|updatedAt|id)$",
              message = "정렬 형식은 'id|createdAt|updatedAt' 중 하나여야 합니다.")
          String sort,
      @Pattern(regexp = "^(asc|desc)$", message = "정렬 방식은 'asc' 또는 'desc' 중 하나여야 합니다.")
          @RequestParam(defaultValue = "desc", required = false)
          String order) {
    PageResponse<AiPromptTemplateResponse> templates =
        aiPromptTemplateQueryUseCase.findPromptTemplates(page, size, sort, order);
    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, templates));
  }

  /** 특정 AI 프롬프트 템플릿을 조회합니다. */
  @GetMapping("/{promptId}")
  public ResponseEntity<CommonResponse<AiPromptTemplateResponse>> getPromptTemplate(
      @PathVariable Long promptId) {
    AiPromptTemplateResponse template = aiPromptTemplateQueryUseCase.findPromptTemplate(promptId);
    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, template));
  }

  /** 새로운 AI 프롬프트 템플릿을 생성합니다. */
  @PostMapping
  public ResponseEntity<CommonResponse<AiPromptTemplateResponse>> createPromptTemplate(
      @RequestBody CreateAiPromptTemplateRequest request) {
    AiPromptTemplateResponse response =
        aiPromptTemplateCommandUseCase.createPromptTemplate(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(CommonResponse.success(HttpStatus.CREATED, response));
  }

  /** 특정 AI 프롬프트 템플릿의 정보를 부분적으로 수정합니다. */
  @PatchMapping("/{promptId}")
  public ResponseEntity<Void> updatePromptTemplate(
      @PathVariable Long promptId, @RequestBody UpdateAiPromptTemplateRequest request) {
    aiPromptTemplateCommandUseCase.updatePromptTemplate(promptId, request);
    return ResponseEntity.ok().build();
  }

  /** 특정 AI 프롬프트 템플릿을 삭제합니다. */
  @DeleteMapping("/{promptId}")
  public ResponseEntity<Void> deletePromptTemplate(@PathVariable Long promptId) {
    aiPromptTemplateCommandUseCase.deletePromptTemplate(promptId);
    return ResponseEntity.noContent().build();
  }
}
