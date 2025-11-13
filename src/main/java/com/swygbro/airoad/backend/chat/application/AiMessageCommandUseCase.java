package com.swygbro.airoad.backend.chat.application;

import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageCreateRequest;
import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageResponse;

public interface AiMessageCommandUseCase {

  ChatMessageResponse saveMessage(Long chatRoomId, ChatMessageCreateRequest request);
}
