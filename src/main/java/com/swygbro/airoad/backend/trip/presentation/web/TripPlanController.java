package com.swygbro.airoad.backend.trip.presentation.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "TripPlan", description = "여행 일정 관리 API")
@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Validated
public class TripPlanController {

  @Operation(
      summary = "사용자의 여행 일정 목록 조회",
      description = """
          로그인한 사용자의 여행 일정 목록을 커서 기반 페이지네이션으로 조회합니다.
          """,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = {
                  @ExampleObject(
                      name = "첫 페이지 조회 성공(다음 페이지 존재)",
                      value =
                          """
                    {
                      "success": true,
                      "status": 200,
                      "data": {
                        "content": [
                          {
                            "id": 15,
                            "title": "제주도 힐링 여행",
                            "startDate": "2025-03-01",
                            "region": "제주",
                            "imageUrl": "https://example.com/images/jeju.jpg"
                          },
                          {
                            "id": 14,
                            "title": "부산 맛집 투어",
                            "startDate": "2025-02-15",
                            "region": "부산",
                            "imageUrl": "https://example.com/images/busan.jpg"
                          },
                          {
                            "id": 13,
                            "title": "서울 3박 4일",
                            "startDate": "2025-01-20",
                            "region": "서울",
                            "imageUrl": "https://example.com/images/seoul.jpg"
                          }
                        ],
                        "nextCursor": 13,
                        "hasNext": true,
                        "size": 3
                      }
                    }
                    """),
                  @ExampleObject(
                      name = "마지막 페이지 조회",
                      value =
                          """
                    {
                      "success": true,
                      "status": 200,
                      "data": {
                        "content": [
                          {
                            "id": 12,
                            "title": "강릉 바다 여행",
                            "startDate": "2024-12-10",
                            "region": "강릉",
                            "imageUrl": "https://example.com/images/gangneung.jpg"
                          }
                        ],
                        "nextCursor": null,
                        "hasNext": false,
                        "size": 1
                      }
                    }
                    """),
                  @ExampleObject(
                      name = "여행 일정이 없는 경우",
                      value =
                          """
                    {
                      "success": true,
                      "status": 200,
                      "data": {
                        "content": [],
                        "nextCursor": null,
                        "hasNext": false,
                        "size": 0
                      }
                    }
                    """)
                })),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "유효하지 않은 페이지 크기",
                      value =
                          """
                    {
                      "success": false,
                      "status": 400,
                      "data": {
                        "timestamp": "2025-10-30T10:00:00",
                        "code": "COMMON002",
                        "message": "잘못된 요청입니다.",
                        "path": "/api/v1/trips",
                        "errors": [
                          {
                            "field": "size",
                            "rejectedValue": 101,
                            "message": "페이지 크기는 1 이상 100 이하여야 합니다."
                          }
                        ]
                      }
                    }
                    """),
                  @ExampleObject(
                      name = "유효하지 않은 정렬 형식",
                      value =
                          """
                    {
                      "success": false,
                      "status": 400,
                      "data": {
                        "timestamp": "2025-10-30T10:00:00",
                        "code": "COMMON002",
                        "message": "잘못된 요청입니다.",
                        "path": "/api/v1/trips",
                        "errors": [
                          {
                            "field": "sort",
                            "rejectedValue": "invalidSort",
                            "message": "정렬 형식은 'field:direction' 형태여야 합니다. (예: createdAt:desc)"
                          }
                        ]
                      }
                    }
                    """)
                })),
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
                    "timestamp": "2025-10-30T10:00:00",
                    "code": "AUTH001",
                    "message": "인증이 필요합니다.",
                    "path": "/api/v1/trips",
                    "errors": null
                  }
                }
                """)))
  })
  @GetMapping
  public ResponseEntity<CommonResponse<CursorPageResponse<TripPlanResponse>>> getUserTripPlans(
      @Parameter(description = "페이지당 조회할 여행 일정 개수", example = "10")
          @RequestParam(defaultValue = "10")
          @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
          @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
          int size,
      @Parameter(description = "페이징 커서 (이전 페이지의 마지막 여행 일정 ID, 없으면 첫 페이지 조회)", example = "13")
          @RequestParam(required = false)
          Long cursor,
      @Parameter(
              description =
                  "정렬 기준 (형식: field:direction). 지원 필드: createdAt, startDate. 방향: asc, desc",
              example = "createdAt:desc")
          @RequestParam(required = false)
          String sort) {

    return ResponseEntity.ok().build();
  }
}
