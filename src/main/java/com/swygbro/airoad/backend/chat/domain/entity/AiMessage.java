package com.swygbro.airoad.backend.chat.domain.entity;

import jakarta.persistence.*;

import org.springframework.ai.chat.messages.MessageType;

import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대화 세션에 포함된 개별 메시지를 나타내는 엔티티
 *
 * <p><strong>ID 순서 보장</strong>: 이 엔티티는 {@link GenerationType#IDENTITY} 전략을 사용하여 ID를 생성합니다.
 * PostgreSQL의 SERIAL/BIGSERIAL은 단조 증가를 보장하므로, ID가 작을수록 먼저 생성된 메시지입니다. 따라서 {@code ORDER BY id DESC}는
 * 시간 역순 정렬과 동일합니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiMessage extends BaseEntity {

  /** 메시지가 속한 대화 세션 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private AiConversation conversation;

  /** 메시지 발신자 */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MessageType messageType;

  /** 메시지 내용 */
  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  @Builder
  private AiMessage(AiConversation conversation, MessageType messageType, String content) {
    this.conversation = conversation;
    this.messageType = messageType;
    this.content = content;
  }
}
