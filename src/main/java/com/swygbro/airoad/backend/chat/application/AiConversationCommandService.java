package com.swygbro.airoad.backend.chat.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.chat.exception.ChatErrorCode;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiConversationRepository;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiMessageRepository;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiConversationCommandService implements AiConversationCommandUseCase {

  private final AiConversationRepository aiConversationRepository;
  private final AiMessageRepository aiMessageRepository;

  @Override
  @Transactional
  public void deleteConversation(Long conversationId) {
    if (!aiConversationRepository.existsById(conversationId)) {
      throw new BusinessException(ChatErrorCode.CONVERSATION_NOT_FOUND);
    }

    aiMessageRepository.deleteByConversationId(conversationId);
    aiConversationRepository.deleteById(conversationId);
  }
}
