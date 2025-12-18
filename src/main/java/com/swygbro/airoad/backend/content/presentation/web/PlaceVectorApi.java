package com.swygbro.airoad.backend.content.presentation.web;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Place Vector", description = "장소 데이터 임베딩 관리 API (관리자 전용)")
public interface PlaceVectorApi {

  @Operation(
      summary = "장소 데이터 벡터 임베딩 생성",
      description = """
            장소(Place) 데이터를 임베딩 처리하여 벡터 스토어에 저장합니다.
            """,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "임베딩 처리 완료 (No Content)"),
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
                                "path": "/api/v1/admin/places/vectors",
                                "errors": [
                                  {
                                    "field": "since",
                                    "rejectedValue": "invalid-date",
                                    "message": "날짜 형식이 올바르지 않습니다. ISO 8601 형식을 사용하세요."
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
                                "path": "/api/v1/admin/places/vectors",
                                "errors": null
                              }
                            }
                            """)))
  })
  ResponseEntity<CommonResponse<?>> createPlacesVector(
      @Parameter(description = "이 시간 이후에 수정된 장소만 처리 (ISO 8601 형식)", example = "2025-01-01T00:00:00")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime since);

  @Operation(
      summary = "특정 장소 벡터 임베딩 생성",
      description = """
            지정된 장소(ID)에 대해 임베딩을 생성하고 이를 벡터 스토어에 저장합니다.
            """,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "임베딩 처리 완료 (No Content)"),
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
                                "path": "/api/v1/admin/places/1/vectors",
                                "errors": null
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "404",
        description = "장소를 찾을 수 없음",
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
                                "code": "PLACE001",
                                "message": "장소를 찾을 수 없습니다.",
                                "path": "/api/v1/admin/places/999/vectors",
                                "errors": null
                              }
                            }
                            """)))
  })
  ResponseEntity<CommonResponse<?>> createPlaceVector(
      @Parameter(description = "임베딩을 생성할 장소 ID", required = true, example = "1") @PathVariable
          Long placeId);
}
