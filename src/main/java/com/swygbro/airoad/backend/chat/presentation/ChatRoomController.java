package com.swygbro.airoad.backend.chat.presentation;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageDto;
import com.swygbro.airoad.backend.chat.domain.dto.ChatRoomDto;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;
import com.swygbro.airoad.backend.common.domain.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "ChatRoom", description = "채팅방 관리 API")
@RestController
@RequestMapping("/api/v1/chatroom")
@RequiredArgsConstructor
public class ChatRoomController {

  @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성합니다.")
  @PostMapping
  public ResponseEntity<CommonResponse<ChatRoomDto.Response>> createChatRoom(
      @Valid @RequestBody ChatRoomDto.CreateRequest createRequest) {
    // TODO: Implement
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(CommonResponse.success(HttpStatus.CREATED, null));
  }

  @Operation(summary = "채팅방 목록 조회", description = "사용자의 채팅방 목록을 페이지네이션으로 조회합니다.")
  @GetMapping("/{memberId}")
  public ResponseEntity<CommonResponse<PageResponse<ChatRoomDto.Response>>> getChatRooms(
      @Parameter(description = "회원 ID", example = "1") @PathVariable Long memberId,
      @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20")
          int size) {
    // TODO: Implement
    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, null));
  }

  @Operation(
      summary = "채팅방 메시지 이력 조회",
      description =
          "채팅방의 과거 메시지를 커서 기반 페이징으로 조회합니다. "
              + "cursor가 없으면 최신 메시지부터, 있으면 해당 메시지 이전의 메시지를 반환합니다. "
              + "실시간 메시지는 WebSocket(/sub/chatroom/{roomId})을 구독하여 수신합니다.")
  @GetMapping("/{chatRoomId}/messages")
  public ResponseEntity<CommonResponse<CursorPageResponse<ChatMessageDto.Response>>> getMessages(
      @Parameter(description = "채팅방 ID", example = "1") @PathVariable Long chatRoomId,
      @Parameter(description = "조회할 메시지 개수", example = "50") @RequestParam(defaultValue = "50")
          int size,
      @Parameter(description = "커서 (이 메시지 ID 이전의 메시지를 조회, 없으면 최신 메시지부터)")
          @RequestParam(required = false)
          Long cursor) {
    // TODO: Implement
    // chatMessageService.getMessageHistory(roomId, cursor, size);
    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, null));
  }
}
