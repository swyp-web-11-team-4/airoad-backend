package com.swygbro.airoad.backend.ai.application.command;

import com.swygbro.airoad.backend.ai.domain.dto.request.CreateAiPromptTemplateRequest;
import com.swygbro.airoad.backend.ai.domain.dto.request.UpdateAiPromptTemplateRequest;
import com.swygbro.airoad.backend.ai.domain.dto.response.AiPromptTemplateResponse;

/** AI 프롬프트 템플릿의 상태를 변경하는 Command(명령) 관련 비즈니스 로직을 정의하는 UseCase 인터페이스 */
public interface AiPromptTemplateCommandUseCase {

  /**
   * 새로운 AI 프롬프트 템플릿을 생성합니다.
   *
   * @param request 생성할 템플릿의 정보를 담은 DTO
   * @return 생성된 템플릿의 정보를 담은 DTO
   */
  AiPromptTemplateResponse createPromptTemplate(CreateAiPromptTemplateRequest request);

  /**
   * 특정 AI 프롬프트 템플릿의 정보를 부분적으로 수정합니다.
   *
   * @param promptId 정보를 수정할 템플릿의 ID
   * @param request 수정할 정보가 담긴 DTO
   */
  void updatePromptTemplate(Long promptId, UpdateAiPromptTemplateRequest request);

  /**
   * 특정 AI 프롬프트 템플릿을 삭제합니다.
   *
   * @param promptId 삭제할 템플릿의 ID
   */
  void deletePromptTemplate(Long promptId);
}
