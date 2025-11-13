package com.swygbro.airoad.backend.chat.presentation.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swygbro.airoad.backend.auth.domain.dto.UserPrincipal;
import com.swygbro.airoad.backend.chat.application.AiMessageService;
import com.swygbro.airoad.backend.chat.domain.dto.response.ChatMessageResponse;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;

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

@Tag(name = "ChatRoom", description = "채팅방 관리 API")
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class AiConversationController {
  private final AiMessageService aiMessageService;

  @Operation(
      summary = "채팅방 메시지 이력 조회",
      description =
          "커서 기반으로 과거 메시지를 조회합니다. cursor가 없으면 최신부터, 있으면 해당 ID 이전 메시지를 반환합니다. 실시간 수신은 /sub/chatroom/{roomId} 구독.",
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
                      name = "페이로드 예시(다음 페이지 존재)",
                      value =
                          """
                    {
                      "success": true,
                      "status": 200,
                      "data": {
                        "content": [
                          {
                            "id": 2,
                            "messageType": "USER",
                            "content": "서울 3박 4일 여행 계획을 짜주세요",
                            "messageContentType": "TEXT",
                            "mediaUrl": null,
                            "createdAt": "2025-01-15T10:31:00"
                          },
                          {
                            "id": 1,
                            "messageType": "ASSISTANT",
                            "content": "서울 3박 4일 여행 계획을 제안드립니다.",
                            "messageContentType": "TEXT",
                            "mediaUrl": null,
                            "createdAt": "2025-01-15T10:32:00"
                          }
                        ],
                        "nextCursor": 1,
                        "hasNext": true,
                        "size": 2
                      }
                    }
                    """),
                  @ExampleObject(
                      name = "빈 결과(마지막 페이지)",
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
                      name = "유효하지 않은 커서",
                      value =
                          """
                    {
                      "success": false,
                      "status": 400,
                      "error": { "code": "CHAT201", "message": "유효하지 않은 커서입니다." }
                    }
                    """),
                  @ExampleObject(
                      name = "유효하지 않은 페이지 크기",
                      value =
                          """
                    {
                      "success": false,
                      "status": 400,
                      "error": { "code": "CHAT202", "message": "페이지 크기는 1 이상 100 이하여야 합니다." }
                    }
                    """)
                })),
    @ApiResponse(
        responseCode = "404",
        description = "존재하지 않는 채팅방",
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
                  "error": { "code": "CHAT301", "message": "존재하지 않는 채팅방입니다." }
                }
                """)))
  })
  @GetMapping("/{chatRoomId}/messages")
  public ResponseEntity<CommonResponse<CursorPageResponse<ChatMessageResponse>>> getMessages(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Parameter(description = "채팅방 ID", example = "1") @PathVariable Long chatRoomId,
      @Parameter(description = "조회할 메시지 개수", example = "50") @RequestParam(defaultValue = "50")
          int size,
      @Parameter(description = "커서 (이 메시지 ID 이전의 메시지를 조회, 없으면 최신 메시지부터)")
          @RequestParam(required = false)
          Long cursor) {
    String username = userPrincipal.getUsername();
    CursorPageResponse<ChatMessageResponse> messageHistory =
        aiMessageService.getMessageHistory(chatRoomId, username, cursor, size);
    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, messageHistory));
  }
}
