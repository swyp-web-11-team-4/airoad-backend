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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class AiConversationController implements AiConversationApi {
  private final AiMessageService aiMessageService;

  @Override
  @GetMapping("/{chatRoomId}/messages")
  public ResponseEntity<CommonResponse<CursorPageResponse<ChatMessageResponse>>> getMessages(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long chatRoomId,
      @RequestParam(defaultValue = "50") int size,
      @RequestParam(required = false) Long cursor) {
    String username = userPrincipal.getUsername();
    CursorPageResponse<ChatMessageResponse> messageHistory =
        aiMessageService.getMessageHistory(chatRoomId, username, cursor, size);
    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, messageHistory));
  }
}
