package com.swygbro.airoad.backend.ai.application.query;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.ai.domain.dto.response.AiPromptTemplateResponse;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.entity.AiPromptTemplate;
import com.swygbro.airoad.backend.ai.domain.entity.PromptType;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.ai.infrastructure.repository.AiPromptTemplateRepository;
import com.swygbro.airoad.backend.common.domain.dto.PageResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** AI 프롬프트 템플릿을 조회하는 Query(조회) 관련 비즈니스 로직을 구현하는 서비스 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiPromptTemplateQueryService implements AiPromptTemplateQueryUseCase {

  private final AiPromptTemplateRepository aiPromptTemplateRepository;

  @Override
  public PageResponse<AiPromptTemplateResponse> findPromptTemplates(
      int page, int size, String sort, String order) {
    Sort sortBy = Sort.by(Sort.Direction.fromString(order), sort);
    PageRequest pageRequest = PageRequest.of(page, size, sortBy);

    PageResponse<AiPromptTemplateResponse> response =
        PageResponse.of(
            aiPromptTemplateRepository.findAll(pageRequest).map(AiPromptTemplateResponse::of));
    log.debug("{}개의 AI 프롬프트 템플릿을 조회했습니다.", response.totalElements());
    return response;
  }

  @Override
  public AiPromptTemplateResponse findPromptTemplate(Long promptId) {
    AiPromptTemplate template =
        aiPromptTemplateRepository
            .findById(promptId)
            .orElseThrow(() -> new BusinessException(AiErrorCode.TEMPLATE_NOT_FOUND));
    log.debug("AI 프롬프트 템플릿 ID {} 조회를 성공했습니다.", promptId);
    return AiPromptTemplateResponse.of(template);
  }

  @Override
  public AiPromptTemplateResponse findActivePromptTemplate(
      PromptType promptType, AgentType agentType) {
    AiPromptTemplate template =
        aiPromptTemplateRepository
            .findByActivePrompt(promptType, agentType)
            .orElseThrow(() -> new BusinessException(AiErrorCode.TEMPLATE_NOT_FOUND));
    return AiPromptTemplateResponse.of(template);
  }
}
