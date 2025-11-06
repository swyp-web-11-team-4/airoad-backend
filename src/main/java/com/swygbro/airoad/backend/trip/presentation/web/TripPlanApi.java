package com.swygbro.airoad.backend.trip.presentation.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
                                      "memberId": 1,
                                      "chatRoomId": 105,
                                      "title": "제주도 힐링 여행",
                                      "startDate": "2025-03-01",
                                      "region": "제주",
                                      "imageUrl": "https://example.com/images/jeju.jpg"
                                    },
                                    {
                                      "id": 14,
                                      "memberId": 1,
                                      "chatRoomId": 104,
                                      "title": "부산 맛집 투어",
                                      "startDate": "2025-02-15",
                                      "region": "부산",
                                      "imageUrl": "https://example.com/images/busan.jpg"
                                    },
                                    {
                                      "id": 13,
                                      "memberId": 1,
                                      "chatRoomId": 103,
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
                                      "memberId": 1,
                                      "chatRoomId": 102,
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
      summary = "여행 일정 생성 세션 생성",
      description =
          """
                    AI 기반 여행 일정 생성을 위한 세션을 생성합니다.
                    사용자가 제공한 여행 조건으로 chatRoom과 TripPlan을 생성하고, WebSocket 구독에 필요한 채널 ID를 반환합니다.

                    ## 처리 흐름
                    1. **이 API 호출** → 여행 조건(request body)과 함께 요청
                    2. 서버: AiConversation, TripPlan 생성 및 ID 반환
                    3. 클라이언트: 반환받은 ID로 WebSocket 채널 구독
                       - `/user/sub/chat/{chatRoomId}`
                       - `/user/sub/schedule/{tripPlanId}`
                       - `/user/sub/errors/{chatRoomId}`
                    4. 클라이언트: POST /api/v1/trips/{chatRoomId} 호출
                    5. 서버: AI 일정 생성 시작 및 WebSocket으로 실시간 스트리밍

                    ## Request Body
                    - themes: 여행 테마 목록 (PlaceThemeType enum 배열)
                      - FAMOUS_SPOT: 유명 관광지
                      - HEALING: 힐링
                      - SNS_HOTSPOT: SNS 핫플
                      - EXPERIENCE_ACTIVITY: 체험 액티비티
                      - CULTURE_ART: 문화/예술
                      - SHOPPING: 쇼핑
                      - RESTAURANT: 음식점
                    - startDate: 여행 시작 날짜 (YYYY-MM-DD)
                    - duration: 여행 기간 (일 단위, 최소 1일)
                    - region: 여행 지역 (예: "제주", "서울", "부산")
                    - peopleCount: 여행 인원 (최소 1명)

                    ## 주의사항
                    - 이 API는 세션만 생성하며, 실제 AI 일정 생성은 시작하지 않습니다
                    - 반환된 chatRoomId tripPlanId로 WebSocket 구독 후 start API를 호출해야 합니다
                    """,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "202",
        description = "AiConversation 및 TripPlan 생성 완료",
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
                                        "aiConversationId": 123,
                                        "tripPlanId": 456
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
                                    "path": "/api/v1/trips",
                                    "errors": null
                                  }
                                }
                                """)))
  })
  @PostMapping
  ResponseEntity<CommonResponse<Object>> generateTripPlan(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Valid @RequestBody TripPlanCreateRequest request);

  @Operation(
      summary = "AI 여행 일정 생성 시작",
      description =
          """
                    클라이언트가 WebSocket 채널 구독을 완료한 후 호출하는 API입니다.
                    TripPlanGenerationRequestedEvent를 발행하여 AI 기반 여행 일정 생성 프로세스를 시작합니다.

                    ## 호출 시점
                    1. POST /api/v1/trips 호출 → 여행 조건(request body)과 함께 요청
                    2. 서버 응답으로 chatRoomId와 tripPlanId 수신
                    3. WebSocket 채널 구독 완료
                       - `/user/sub/chat/{chatRoomId}`
                       - `/user/sub/schedule/{tripPlanId}`
                       - `/user/sub/errors/{chatRoomId}`
                    4. **이 API 호출** → chatRoomId를 경로 변수로 전달
                    5. 서버: TripPlanGenerationRequestedEvent 발행
                    6. AI 리스너: 이벤트 수신하여 일정 생성 시작
                    7. WebSocket으로 실시간 스트리밍 응답 전송

                    ## WebSocket 메시지 수신
                    ### 채팅 채널 (`/user/sub/chat/{chatRoomId}`)
                    - AI 스트리밍 응답 (여행 일정 생성 과정 설명)
                    - 완료/취소 알림 (`COMPLETED`, `CANCELLED`)

                    ### 일정 전송 채널 (`/user/sub/schedule/{tripPlanId}`)
                    - 일차별 일정(DailyPlan) 저장 완료 데이터
                    - 각 일차의 방문지, 이동 정보 포함

                    ### 에러 채널 (`/user/sub/errors/{chatRoomId}`)
                    - 일정 생성 중 발생한 오류 알림

                    ## 주의사항
                    - 구독 완료 전 이 API를 호출하면 메시지 손실 위험이 있습니다
                    - chatRoomId는 POST /api/v1/trips 응답의 chatRoomId 값을 사용하세요
                    - 여행 조건은 이미 TripPlan에 저장되어 있으므로 별도로 전달하지 않습니다
                    """,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "AI 일정 생성 시작",
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
                                      "status": 200,
                                      "data": {
                                        "message": "여행 일정 생성을 시작합니다."
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
                                    "path": "/api/v1/trips/{chatRoomId}",
                                    "errors": null
                                  }
                                }
                                """))),
    @ApiResponse(
        responseCode = "404",
        description = "채팅방을 찾을 수 없음",
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
                                    "code": "CHAT001",
                                    "message": "채팅방을 찾을 수 없습니다.",
                                    "path": "/api/v1/trips/{chatRoomId}",
                                    "errors": null
                                  }
                                }
                                """)))
  })
  @PostMapping("/{tripPlanId}")
  ResponseEntity<CommonResponse<Object>> startTripPlanGeneration(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Parameter(description = "채팅방 ID (chatRoomId)", example = "123", required = true)
          @PathVariable
          Long tripPlanId);
}
