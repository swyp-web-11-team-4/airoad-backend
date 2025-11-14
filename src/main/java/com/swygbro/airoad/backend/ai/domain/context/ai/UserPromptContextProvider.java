package com.swygbro.airoad.backend.ai.domain.context.ai;

import java.util.List;

import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.application.query.AiPromptTemplateQueryUseCase;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor.MetadataEntry;
import com.swygbro.airoad.backend.ai.common.context.AbstractContextProvider;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.entity.PromptType;

import lombok.extern.slf4j.Slf4j;

/**
 * 유저 프롬프트를 제공하는 ContextProvider
 *
 * <p>AgentType에 따라 데이터베이스에서 활성화된 유저 프롬프트를 조회하여 컨텍스트로 제공합니다.
 */
@Slf4j
@Component
public class UserPromptContextProvider extends AbstractContextProvider<AgentType> {

  private final AiPromptTemplateQueryUseCase promptTemplateQueryUseCase;

  public UserPromptContextProvider(AiPromptTemplateQueryUseCase promptTemplateQueryUseCase) {
    super(AgentType.class);
    this.promptTemplateQueryUseCase = promptTemplateQueryUseCase;
  }

  @Override
  protected List<MetadataEntry> doGetContext(AgentType agentType) {
    log.debug("유저 프롬프트 조회 - AgentType: {}", agentType);

    String systemPrompt =
        promptTemplateQueryUseCase.findActivePromptTemplate(PromptType.USER, agentType).prompt();

    log.debug("유저 프롬프트 조회 완료 - 길이: {} 자", systemPrompt.length());

    return PromptMetadataAdvisor.userMetadata(systemPrompt);
  }

  @Override
  public int getOrder() {
    return 1;
  }
}
