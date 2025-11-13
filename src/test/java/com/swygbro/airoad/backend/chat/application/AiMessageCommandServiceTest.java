package com.swygbro.airoad.backend.chat.application;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.MessageType;

import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageCreateRequest;
import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageResponse;
import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.chat.domain.entity.AiMessage;
import com.swygbro.airoad.backend.chat.exception.ChatErrorCode;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiConversationRepository;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiMessageRepository;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.fixture.chat.AiConversationFixture;
import com.swygbro.airoad.backend.fixture.chat.AiMessageFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiMessageCommandServiceTest {

  @InjectMocks private AiMessageCommandService aiMessageCommandService;

  @Mock private AiConversationRepository aiConversationRepository;

  @Mock private AiMessageRepository aiMessageRepository;

  @Test
  @DisplayName("채팅 메시지 저장 요청 시, 메시지가 성공적으로 저장되어야 한다.")
  void saveMessage_Success() {
    // given
    Long chatRoomId = 1L;
    ChatMessageCreateRequest request = new ChatMessageCreateRequest("안녕하세요", MessageType.USER);
    AiConversation conversation = AiConversationFixture.create();
    AiMessage message = AiMessageFixture.createWithConversation(conversation);

    given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(conversation));
    given(aiMessageRepository.save(any(AiMessage.class))).willReturn(message);

    // when
    ChatMessageResponse response = aiMessageCommandService.saveMessage(chatRoomId, request);

    // then
    assertThat(response).isNotNull();
    assertThat(response.content()).isEqualTo(message.getContent());
    assertThat(response.messageType()).isEqualTo(message.getMessageType());
    verify(aiConversationRepository).findById(chatRoomId);
    verify(aiMessageRepository).save(any(AiMessage.class));
  }

  @Test
  @DisplayName("존재하지 않는 채팅방 ID로 메시지 저장 요청 시, BusinessException이 발생해야 한다.")
  void saveMessage_ConversationNotFound_ThrowsBusinessException() {
    // given
    Long invalidChatRoomId = 999L;
    ChatMessageCreateRequest request = new ChatMessageCreateRequest("안녕하세요", MessageType.USER);
    given(aiConversationRepository.findById(invalidChatRoomId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> aiMessageCommandService.saveMessage(invalidChatRoomId, request))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ChatErrorCode.CONVERSATION_NOT_FOUND);
    verify(aiConversationRepository).findById(invalidChatRoomId);
  }
}
