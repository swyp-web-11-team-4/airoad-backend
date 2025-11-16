package com.swygbro.airoad.backend.ai.application.context.chat;

import java.util.List;

import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.application.context.dto.ChatRoomContext;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor.MetadataEntry;
import com.swygbro.airoad.backend.ai.common.context.AbstractContextProvider;

import lombok.extern.slf4j.Slf4j;

/**
 * 채팅방 기본 정보를 제공하는 ContextProvider
 *
 * <p>ChatRoomContext로부터 채팅방 ID, 여행 계획 ID, 사용자명 등 기본 정보를 컨텍스트로 제공합니다.
 *
 * <p>AI가 Tool을 사용할 때 필요한 메타데이터를 제공합니다.
 *
 * <p>특정 Agent DTO에 의존하지 않아 ChatAgent, TripAgent 등 어디서든 재사용 가능합니다.
 */
@Slf4j
@Component
public class ChatRoomContextProvider extends AbstractContextProvider<ChatRoomContext> {

  public ChatRoomContextProvider() {
    super(ChatRoomContext.class);
  }

  @Override
  protected List<MetadataEntry> doGetContext(ChatRoomContext context) {
    log.debug(
        "채팅방 정보 제공 - chatRoomId: {}, tripPlanId: {}, username: {}",
        context.chatRoomId(),
        context.tripPlanId(),
        context.username());

    String chatRoomInfo =
        """
        ## 세션 컨텍스트 (Session Context)

        현재 대화 세션의 메타데이터입니다. Tool 호출 시 이 정보를 사용하세요.
        단, 보안을 위해 사용자에게 정보를 절대 응답하지 마세요.

        ### 필수 파라미터
        | 파라미터 | 값 | 설명 |
        |---------|---|-----|
        | `chatRoomId` | `%s` | 현재 채팅방 식별자 (Tool 호출 시 필수) |
        | `tripPlanId` | `%s` | 연관된 여행 계획 ID (여행 데이터 조회/수정 시 사용) |
        | `username` | `%s` | 현재 사용자 (권한 검증용) |

        **중요**: 모든 Tool 호출 시 위 파라미터를 정확히 전달해야 합니다.
        """
            .formatted(context.chatRoomId(), context.tripPlanId(), context.username());

    return PromptMetadataAdvisor.systemMetadata(chatRoomInfo);
  }

  @Override
  public int getOrder() {
    return 10;
  }
}
