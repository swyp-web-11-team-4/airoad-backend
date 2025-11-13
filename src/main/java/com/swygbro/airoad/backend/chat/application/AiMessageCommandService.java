package com.swygbro.airoad.backend.chat.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageCreateRequest;
import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageResponse;
import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.chat.domain.entity.AiMessage;
import com.swygbro.airoad.backend.chat.exception.ChatErrorCode;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiConversationRepository;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiMessageRepository;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AiMessageCommandService implements AiMessageCommandUseCase {

  private final AiConversationRepository aiConversationRepository;
  private final AiMessageRepository aiMessageRepository;

  @Override
  public ChatMessageResponse saveMessage(Long chatRoomId, ChatMessageCreateRequest request) {
    AiConversation aiConversation =
        aiConversationRepository
            .findById(chatRoomId)
            .orElseThrow(() -> new BusinessException(ChatErrorCode.CONVERSATION_NOT_FOUND));

    AiMessage aiMessage =
        AiMessage.builder()
            .conversation(aiConversation)
            .content(request.message())
            .messageType(request.messageType())
            .build();

    aiMessage = aiMessageRepository.save(aiMessage);
    return ChatMessageResponse.from(aiMessage);
  }
}
