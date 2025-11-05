package com.swygbro.airoad.backend.trip.presentation.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.swygbro.airoad.backend.auth.domain.dto.UserPrincipal;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanUpdateRequest;
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

@Tag(name = "TripPlan", description = "여행 일정 관리 API")
public interface TripPlanApi {

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
  ResponseEntity<CommonResponse<CursorPageResponse<TripPlanResponse>>> getUserTripPlans(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Parameter(description = "페이지당 조회할 여행 일정 개수", example = "10")
          @RequestParam(defaultValue = "10")
          @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
          @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
          int size,
      @Parameter(description = "페이징 커서 (이전 페이지의 마지막 여행 일정 ID, 없으면 첫 페이지 조회)")
          @RequestParam(required = false)
          @Positive(message = "커서 값은 1 이상이어야 합니다.")
          Long cursor,
      @Parameter(
              description =
                  "정렬 기준 (형식: field:direction). 지원 필드: createdAt, startDate. 방향: asc, desc",
              example = "createdAt:desc")
          @Pattern(
              regexp = "^(createdAt|startDate):(asc|desc)$",
              message = "정렬 형식은 'field:direction' 형태여야 합니다.")
          @RequestParam(required = false, defaultValue = "createdAt:desc")
          String sort);

  @Operation(
      summary = "여행 일정 제목 수정",
      description = "소유자만 본인의 여행 일정 제목을 수정할 수 있습니다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "제목 수정 성공"),
    @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
    @ApiResponse(responseCode = "404", description = "여행 일정을 찾을 수 없음")
  })
  ResponseEntity<Void> updateTripPlanTitle(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Parameter(description = "수정할 여행 일정 ID", required = true) @PathVariable Long tripPlanId,
      @Valid @RequestBody TripPlanUpdateRequest request);

  @Operation(
      summary = "여행 일정 삭제",
      description = "소유자만 본인의 여행 일정을 삭제할 수 있습니다.",
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
                              "timestamp": "2025-10-30T10:00:00",
                              "code": "AUTH001",
                              "message": "인증이 필요합니다.",
                              "path": "/api/v1/trips/123",
                              "errors": null
                            }
                          }
                          """))),
    @ApiResponse(
        responseCode = "403",
        description = "접근 권한 없음",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                          {
                            "success": false,
                            "status": 403,
                            "data": {
                              "timestamp": "2025-10-30T10:00:00",
                              "code": "TRIP102",
                              "message": "여행 일정에 대한 접근 권한이 없습니다.",
                              "path": "/api/v1/trips/123",
                              "errors": null
                            }
                          }
                          """))),
    @ApiResponse(
        responseCode = "404",
        description = "여행 일정을 찾을 수 없음",
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
                              "timestamp": "2025-10-30T10:00:00",
                              "code": "TRIP101",
                              "message": "여행 일정을 찾을 수 없습니다.",
                              "path": "/api/v1/trips/123",
                              "errors": null
                            }
                          }
                          """)))
  })
  ResponseEntity<Void> deleteTripPlan(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Parameter(description = "삭제할 여행 일정 ID", required = true, example = "123") @PathVariable
          Long tripPlanId);

  @Operation(
      summary = "AI 여행 일정 생성 요청",
      description =
          """
          AI에게 여행 일정 생성을 요청합니다.
          요청이 성공하면 202 Accepted 상태와 함께 생성 시작 메시지를 반환합니다.
          실제 여행 일정 생성은 비동기적으로 처리됩니다.
          """,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "202",
        description = "생성 요청 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                                {
                                  "success": true,
                                  "status": 202,
                                  "data": {
                                    "message": "여행 일정 생성이 시작되었습니다."
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
                        name = "필수 파라미터 누락",
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
                                        "field": "chatRoomId",
                                        "rejectedValue": null,
                                        "message": "채팅방 ID는 필수입니다."
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
                                    "timestamp": "2025-10-30T10:00:00",
                                    "code": "AUTH001",
                                    "message": "인증이 필요합니다.",
                                    "path": "/api/v1/trips",
                                    "errors": null
                                  }
                                }
                                """)))
  })
  ResponseEntity<CommonResponse<Object>> generateTripPlan(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Parameter(description = "채팅방 ID", example = "1", required = true) @RequestParam
          Long chatRoomId,
      @Valid @RequestBody TripPlanCreateRequest request);
}
