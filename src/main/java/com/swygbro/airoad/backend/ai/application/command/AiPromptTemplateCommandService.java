package com.swygbro.airoad.backend.ai.application.command;

import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.ai.domain.dto.request.CreateAiPromptTemplateRequest;
import com.swygbro.airoad.backend.ai.domain.dto.request.UpdateAiPromptTemplateRequest;
import com.swygbro.airoad.backend.ai.domain.dto.response.AiPromptTemplateResponse;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.entity.AiPromptTemplate;
import com.swygbro.airoad.backend.ai.domain.entity.PromptType;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.ai.infrastructure.repository.AiPromptTemplateRepository;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** AI 프롬프트 템플릿의 상태를 변경하는 Command(명령) 관련 비즈니스 로직을 구현하는 서비스 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AiPromptTemplateCommandService implements AiPromptTemplateCommandUseCase {

  private final AiPromptTemplateRepository aiPromptTemplateRepository;

  @Override
  public AiPromptTemplateResponse createPromptTemplate(CreateAiPromptTemplateRequest request) {
    AiPromptTemplate template =
        AiPromptTemplate.builder()
            .promptType(request.promptType())
            .agentType(request.agentType())
            .prompt(request.prompt())
            .description(request.description())
            .build();

    AiPromptTemplate savedTemplate = aiPromptTemplateRepository.save(template);

    log.info("AI 프롬프트 템플릿을 성공적으로 생성했습니다. ID: {}", savedTemplate.getId());
    return AiPromptTemplateResponse.of(savedTemplate);
  }

  @Override
  public void updatePromptTemplate(Long promptId, UpdateAiPromptTemplateRequest request) {
    AiPromptTemplate template =
        aiPromptTemplateRepository
            .findById(promptId)
            .orElseThrow(() -> new BusinessException(AiErrorCode.TEMPLATE_NOT_FOUND));

    applyUpdate(request.isActive(), handlePromptActivation(promptId, template));
    applyUpdate(request.prompt(), template::updatePrompt);
    applyUpdate(request.description(), template::updateDescription);

    log.info("AI 프롬프트 템플릿 ID {}의 부분 수정을 완료했습니다.", promptId);
  }

  @Override
  public void deletePromptTemplate(Long promptId) {
    if (!aiPromptTemplateRepository.existsById(promptId)) {
      throw new BusinessException(AiErrorCode.TEMPLATE_NOT_FOUND);
    }

    aiPromptTemplateRepository.deleteById(promptId);
    log.info("AI 프롬프트 템플릿 ID {}를 성공적으로 삭제했습니다.", promptId);
  }

  /**
   * 값이 null이 아닐 경우에만 Consumer의 업데이트 로직을 실행하는 헬퍼 메서드
   *
   * @param value 업데이트할 값
   * @param updater 업데이트 로직을 담은 Consumer
   * @param <T> 값의 타입
   */
  private <T> void applyUpdate(T value, Consumer<T> updater) {
    if (value != null) {
      updater.accept(value);
    }
  }

  private Consumer<Boolean> handlePromptActivation(Long promptId, AiPromptTemplate template) {
    return (Boolean isActive) -> {
      if (isActive) {
        deactivateCurrentActiveTemplate(
            template.getPromptType(), template.getAgentType(), promptId);
        template.updatePromptStatus(true);
        log.info("ID {}의 활성 상태를 true로 변경했습니다.", promptId);
      } else {
        template.updatePromptStatus(false);
        log.info("ID {}의 활성 상태를 false로 변경했습니다.", promptId);
      }
    };
  }

  /**
   * 동일한 프롬프트 타입과 에이전트 타입을 가진 현재 활성화된 템플릿을 비활성화합니다.
   *
   * @param promptType 프롬프트 타입
   * @param agentType 에이전트 타입
   * @param promptId 현재 활성화하려는 템플릿의 ID (자기 자신은 비활성화하지 않음)
   */
  private void deactivateCurrentActiveTemplate(
      PromptType promptType, AgentType agentType, Long promptId) {
    Optional<AiPromptTemplate> currentlyActiveTemplate =
        aiPromptTemplateRepository.findByActivePrompt(promptType, agentType);

    currentlyActiveTemplate.ifPresent(
        activeTemplate -> {
          if (!activeTemplate.getId().equals(promptId)) {
            activeTemplate.updatePromptStatus(false);
            log.info("다른 활성 템플릿 ID {}를 비활성화했습니다.", activeTemplate.getId());
          }
        });
  }
}
