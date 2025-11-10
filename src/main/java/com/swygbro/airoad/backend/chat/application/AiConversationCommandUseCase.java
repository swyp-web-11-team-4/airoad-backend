package com.swygbro.airoad.backend.chat.application;

public interface AiConversationCommandUseCase {

  /**
   * conversationId에 해당하는 AiConversation과 그에 속한 AiMessage들을 모두 삭제합니다.
   *
   * @param conversationId 삭제할 대화 ID
   */
  void deleteConversation(Long conversationId);
}
