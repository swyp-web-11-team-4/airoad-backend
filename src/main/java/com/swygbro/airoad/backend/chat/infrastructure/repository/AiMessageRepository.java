package com.swygbro.airoad.backend.chat.infrastructure.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swygbro.airoad.backend.chat.domain.entity.AiMessage;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

  /**
   * 채팅방의 메시지 히스토리를 커서 기반으로 조회 (최신순)
   *
   * <p>ID 기준 역순으로 정렬합니다. {@link GenerationType#IDENTITY} 전략에서 ID는 단조 증가하므로, ID 역순 정렬은 생성 시간 역순과
   * 동일합니다.
   *
   * @param conversationId 대화 세션 ID
   * @param cursor 커서 (메시지 ID, null이면 최신 메시지부터)
   * @param pageable 페이지 정보
   * @return 메시지 페이지
   */
  @Query(
      "SELECT m FROM AiMessage m "
          + "WHERE m.conversation.id = :conversationId "
          + "AND (:cursor IS NULL OR m.id < :cursor) "
          + "ORDER BY m.id DESC")
  Slice<AiMessage> findMessageHistoryByCursor(
      @Param("conversationId") Long conversationId,
      @Param("cursor") Long cursor,
      Pageable pageable);

  /**
   * 특정 메시지가 해당 대화방에 속하는지 확인
   *
   * @param messageId 메시지 ID
   * @param conversationId 대화 세션 ID
   * @return 메시지가 해당 대화방에 속하면 true, 아니면 false
   */
  @Query(
      "SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END "
          + "FROM AiMessage m "
          + "WHERE m.id = :messageId AND m.conversation.id = :conversationId")
  boolean existsByIdAndConversationId(
      @Param("messageId") Long messageId, @Param("conversationId") Long conversationId);
}
