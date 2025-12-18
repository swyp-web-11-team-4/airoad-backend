package com.swygbro.airoad.backend.ai.presentation.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.swygbro.airoad.backend.ai.domain.dto.request.CreateAiPromptTemplateRequest;
import com.swygbro.airoad.backend.ai.domain.dto.request.UpdateAiPromptTemplateRequest;
import com.swygbro.airoad.backend.ai.domain.dto.response.AiPromptTemplateResponse;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.common.domain.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Prompt", description = "프롬프트 템플릿 관리 API (관리자 전용)")
public interface AiPromptTemplateAdminApi {

  @Operation(
      summary = "AI 프롬프트 템플릿 목록 조회",
      description =
          """
            AI 프롬프트 템플릿 목록을 페이지네이션하여 조회합니다.
            정렬 기준과 방향을 지정할 수 있습니다.
            """,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "status": 200,
                              "data": {
                                "content": [
                                  {
                                    "id": 1,
                                    "name": "여행 일정 생성 프롬프트",
                                    "template": "사용자가 {region}으로 {duration}일 여행을 계획하고 있습니다...",
                                    "description": "여행 일정을 생성하는 프롬프트 템플릿",
                                    "createdAt": "2025-01-01T00:00:00",
                                    "updatedAt": "2025-01-10T00:00:00"
                                  }
                                ],
                                "page": 0,
                                "size": 10,
                                "totalElements": 1,
                                "totalPages": 1
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "status": 400,
                              "data": {
                                "timestamp": "2025-12-19T10:00:00",
                                "code": "COMMON002",
                                "message": "잘못된 요청입니다.",
                                "path": "/api/v1/admin/prompts",
                                "errors": [
                                  {
                                    "field": "sort",
                                    "rejectedValue": "invalidField",
                                    "message": "정렬 형식은 'id|createdAt|updatedAt' 중 하나여야 합니다."
                                  }
                                ]
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "인증되지 않은 사용자",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "status": 401,
                              "data": {
                                "timestamp": "2025-12-19T10:00:00",
                                "code": "AUTH001",
                                "message": "인증이 필요합니다.",
                                "path": "/api/v1/admin/prompts",
                                "errors": null
                              }
                            }
                            """)))
  })
  ResponseEntity<CommonResponse<PageResponse<AiPromptTemplateResponse>>> getPromptTemplates(
      @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
          @RequestParam(defaultValue = "0")
          @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
          @Max(value = 100, message = "페이지 번호는 100 이하여야 합니다.")
          int page,
      @Parameter(description = "페이지당 조회할 개수", example = "10")
          @RequestParam(defaultValue = "10")
          @Min(value = 1, message = "페이지 사이즈는 1 이상이어야 합니다.")
          @Max(value = 100, message = "페이지 사이즈는 100 이하여야 합니다.")
          int size,
      @Parameter(description = "정렬 기준 필드", example = "createdAt")
          @RequestParam(defaultValue = "createdAt", required = false)
          @Pattern(
              regexp = "^(createdAt|updatedAt|id)$",
              message = "정렬 형식은 'id|createdAt|updatedAt' 중 하나여야 합니다.")
          String sort,
      @Parameter(description = "정렬 방향", example = "desc")
          @Pattern(regexp = "^(asc|desc)$", message = "정렬 방식은 'asc' 또는 'desc' 중 하나여야 합니다.")
          @RequestParam(defaultValue = "desc", required = false)
          String order);

  @Operation(
      summary = "AI 프롬프트 템플릿 상세 조회",
      description = "특정 AI 프롬프트 템플릿의 상세 정보를 조회합니다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AiPromptTemplateResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "status": 200,
                              "data": {
                                "id": 1,
                                "name": "여행 일정 생성 프롬프트",
                                "template": "사용자가 {region}으로 {duration}일 여행을 계획하고 있습니다...",
                                "description": "여행 일정을 생성하는 프롬프트 템플릿",
                                "createdAt": "2025-01-01T00:00:00",
                                "updatedAt": "2025-01-10T00:00:00"
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "인증되지 않은 사용자",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "status": 401,
                              "data": {
                                "timestamp": "2025-12-19T10:00:00",
                                "code": "AUTH001",
                                "message": "인증이 필요합니다.",
                                "path": "/api/v1/admin/prompts/1",
                                "errors": null
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "404",
        description = "프롬프트 템플릿을 찾을 수 없음",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "status": 404,
                              "data": {
                                "timestamp": "2025-12-19T10:00:00",
                                "code": "AI001",
                                "message": "AI 프롬프트 템플릿을 찾을 수 없습니다.",
                                "path": "/api/v1/admin/prompts/999",
                                "errors": null
                              }
                            }
                            """)))
  })
  ResponseEntity<CommonResponse<AiPromptTemplateResponse>> getPromptTemplate(
      @Parameter(description = "프롬프트 템플릿 ID", required = true, example = "1") @PathVariable
          Long promptId);

  @Operation(
      summary = "AI 프롬프트 템플릿 생성",
      description =
          """
            새로운 AI 프롬프트 템플릿을 생성합니다.
            템플릿 이름, 내용, 설명을 포함해야 합니다.
            """,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "생성 성공",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AiPromptTemplateResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "status": 201,
                              "data": {
                                "id": 1,
                                "name": "새 프롬프트 템플릿",
                                "template": "템플릿 내용...",
                                "description": "템플릿 설명",
                                "createdAt": "2025-12-19T10:00:00",
                                "updatedAt": "2025-12-19T10:00:00"
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "status": 400,
                              "data": {
                                "timestamp": "2025-12-19T10:00:00",
                                "code": "COMMON002",
                                "message": "잘못된 요청입니다.",
                                "path": "/api/v1/admin/prompts",
                                "errors": [
                                  {
                                    "field": "name",
                                    "rejectedValue": "",
                                    "message": "프롬프트 템플릿 이름은 필수입니다."
                                  }
                                ]
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "인증되지 않은 사용자",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "status": 401,
                              "data": {
                                "timestamp": "2025-12-19T10:00:00",
                                "code": "AUTH001",
                                "message": "인증이 필요합니다.",
                                "path": "/api/v1/admin/prompts",
                                "errors": null
                              }
                            }
                            """)))
  })
  ResponseEntity<CommonResponse<AiPromptTemplateResponse>> createPromptTemplate(
      @Parameter(description = "프롬프트 템플릿 생성 요청", required = true) @Valid @RequestBody
          CreateAiPromptTemplateRequest request);

  @Operation(
      summary = "AI 프롬프트 템플릿 수정",
      description = """
            특정 AI 프롬프트 템플릿의 정보를 부분적으로 수정합니다.
            """,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "수정 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "status": 400,
                              "data": {
                                "timestamp": "2025-12-19T10:00:00",
                                "code": "COMMON002",
                                "message": "잘못된 요청입니다.",
                                "path": "/api/v1/admin/prompts/1",
                                "errors": null
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "인증되지 않은 사용자",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "status": 401,
                              "data": {
                                "timestamp": "2025-12-19T10:00:00",
                                "code": "AUTH001",
                                "message": "인증이 필요합니다.",
                                "path": "/api/v1/admin/prompts/1",
                                "errors": null
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "404",
        description = "프롬프트 템플릿을 찾을 수 없음",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "status": 404,
                              "data": {
                                "timestamp": "2025-12-19T10:00:00",
                                "code": "AI001",
                                "message": "AI 프롬프트 템플릿을 찾을 수 없습니다.",
                                "path": "/api/v1/admin/prompts/999",
                                "errors": null
                              }
                            }
                            """)))
  })
  ResponseEntity<Void> updatePromptTemplate(
      @Parameter(description = "프롬프트 템플릿 ID", required = true, example = "1") @PathVariable
          Long promptId,
      @Parameter(description = "프롬프트 템플릿 수정 요청", required = true) @Valid @RequestBody
          UpdateAiPromptTemplateRequest request);

  @Operation(
      summary = "AI 프롬프트 템플릿 삭제",
      description = "특정 AI 프롬프트 템플릿을 삭제합니다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "삭제 성공"),
    @ApiResponse(
        responseCode = "401",
        description = "인증되지 않은 사용자",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "status": 401,
                              "data": {
                                "timestamp": "2025-12-19T10:00:00",
                                "code": "AUTH001",
                                "message": "인증이 필요합니다.",
                                "path": "/api/v1/admin/prompts/1",
                                "errors": null
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "404",
        description = "프롬프트 템플릿을 찾을 수 없음",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "status": 404,
                              "data": {
                                "timestamp": "2025-12-19T10:00:00",
                                "code": "AI001",
                                "message": "AI 프롬프트 템플릿을 찾을 수 없습니다.",
                                "path": "/api/v1/admin/prompts/999",
                                "errors": null
                              }
                            }
                            """)))
  })
  ResponseEntity<Void> deletePromptTemplate(
      @Parameter(description = "프롬프트 템플릿 ID", required = true, example = "1") @PathVariable
          Long promptId);
}
