package com.swygbro.airoad.backend.chat.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.swygbro.airoad.backend.chat.exception.ChatErrorCode;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiConversationRepository;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiMessageRepository;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiConversationCommandServiceTest {

  @InjectMocks private AiConversationCommandService aiConversationCommandService;

  @Mock private AiConversationRepository aiConversationRepository;

  @Mock private AiMessageRepository aiMessageRepository;

  @Test
  @DisplayName("conversationId로 대화 삭제 시, 메시지와 대화를 순서대로 삭제한다")
  void deleteConversation_shouldDeleteMessagesAndConversation() {
    // given
    Long conversationId = 1L;
    given(aiConversationRepository.existsById(conversationId)).willReturn(true);

    // when
    aiConversationCommandService.deleteConversation(conversationId);

    // then
    verify(aiMessageRepository).deleteByConversationId(conversationId);
    verify(aiConversationRepository).deleteById(conversationId);
  }

  @Test
  @DisplayName("존재하지 않는 conversationId로 삭제 시도 시 CONVERSATION_NOT_FOUND 예외가 발생한다")
  void deleteConversation_shouldThrowExceptionWhenConversationNotFound() {
    // given
    Long nonExistentConversationId = 99L;
    given(aiConversationRepository.existsById(nonExistentConversationId)).willReturn(false);

    // when & then
    assertThatThrownBy(
            () -> aiConversationCommandService.deleteConversation(nonExistentConversationId))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CONVERSATION_NOT_FOUND);

    verify(aiMessageRepository, never()).deleteByConversationId(nonExistentConversationId);
    verify(aiConversationRepository, never()).deleteById(nonExistentConversationId);
  }
}
