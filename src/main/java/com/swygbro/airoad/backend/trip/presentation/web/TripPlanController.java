package com.swygbro.airoad.backend.trip.presentation.web;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swygbro.airoad.backend.auth.domain.dto.UserPrincipal;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;
import com.swygbro.airoad.backend.trip.application.TripUseCase;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
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

  private final TripUseCase tripUseCase;

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

  @Operation(
      summary = "여행 일정 생성 요청",
      description =
          """
          AI 기반 여행 일정 생성을 요청합니다.
          사용자가 입력한 여행 조건(지역, 날짜, 기간, 테마, 인원)을 바탕으로 AI가 여행 일정을 생성합니다.

          ## 비동기 처리 방식
          이 API는 202 Accepted를 즉시 반환하고, 실제 일정 생성은 백그라운드에서 비동기로 처리됩니다.

          ## WebSocket 실시간 알림
          일정 생성 진행 상황을 실시간으로 받으려면 다음 WebSocket 채널을 구독하세요:

          ### 1. 채팅 채널
          - **경로**: `/user/sub/chat/{chatRoomId}`
          - **용도**: 일정 생성 진행 상황 및 완료/취소 알림
          - **메시지 타입**: AI 스트리밍 응답, `COMPLETED`, `CANCELLED`
          - **설명**: AI가 일정을 생성하면서 실시간으로 채팅 메시지를 스트리밍합니다

          ### 2. 일정 전송 채널
          - **경로**: `/user/sub/schedule/{tripPlanId}`
          - **용도**: 일차별 일정(DailyPlan) 저장 완료 및 데이터 전달
          - **설명**: AI가 생성한 일정을 파싱하여 DailyPlan 엔티티로 저장하면, 저장된 일일 일정 데이터를 이 채널로 전송합니다
          - **데이터**: 저장된 DailyPlan 객체 (dayNumber, activities, places 등 포함)

          ### 3. 에러 채널
          - **경로**: `/user/sub/errors/{chatRoomId}`
          - **용도**: 일정 생성 중 발생한 오류 알림
          - **메시지 타입**: `ERROR`

          ## 메시지 구조 예시
          ```json
          {
            "type": "DAILY_PLAN_GENERATED",
            "tripPlanId": 123,
            "dailyPlan": { "dayNumber": 1, "activities": [...] },
            "message": "1일차 일정이 생성되었습니다."
          }
          ```

          ## 주의사항
          - tripPlanId는 TripPlan 엔티티 생성 후 이벤트를 통해 전달됩니다
          - WebSocket 연결이 필수는 아니지만, 실시간 진행 상황을 받으려면 구독이 필요합니다
          - 일정 생성은 수 초에서 수십 초가 소요될 수 있습니다
          """,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "202",
        description = "여행 일정 생성 요청 접수됨 (비동기 처리)",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
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
                examples = {
                  @ExampleObject(
                      name = "필수 필드 누락",
                      value =
                          """
                    {
                      "success": false,
                      "status": 400,
                      "data": {
                        "timestamp": "2025-10-30T10:00:00",
                        "code": "COMMON002",
                        "message": "잘못된 요청입니다.",
                        "path": "/api/v1/trips/generate",
                        "errors": [
                          {
                            "field": "region",
                            "rejectedValue": null,
                            "message": "여행 지역은 필수입니다."
                          }
                        ]
                      }
                    }
                    """),
                  @ExampleObject(
                      name = "유효하지 않은 값",
                      value =
                          """
                    {
                      "success": false,
                      "status": 400,
                      "data": {
                        "timestamp": "2025-10-30T10:00:00",
                        "code": "COMMON002",
                        "message": "잘못된 요청입니다.",
                        "path": "/api/v1/trips/generate",
                        "errors": [
                          {
                            "field": "duration",
                            "rejectedValue": 0,
                            "message": "여행 기간은 최소 1일 이상이어야 합니다."
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
                    "path": "/api/v1/trips/generate",
                    "errors": null
                  }
                }
                """))),
    @ApiResponse(
        responseCode = "404",
        description = "회원을 찾을 수 없음",
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
                    "code": "MEMBER001",
                    "message": "회원을 찾을 수 없습니다.",
                    "path": "/api/v1/trips/generate",
                    "errors": null
                  }
                }
                """)))
  })
  @PostMapping("")
  public ResponseEntity<CommonResponse<Object>> generateTripPlan(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Parameter(description = "채팅방 ID", example = "1", required = true) @RequestParam
          Long chatRoomId,
      @Valid @RequestBody TripPlanCreateRequest request) {

    String username = userPrincipal.getUsername();
    tripUseCase.requestTripPlanGeneration(username, request, chatRoomId);

    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(
            CommonResponse.success(
                HttpStatus.ACCEPTED.value(), Map.of("message", "여행 일정 생성이 시작되었습니다.")));
  }
}
