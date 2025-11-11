package com.swygbro.airoad.backend.ai.application.command;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.ai.domain.dto.request.CreateAiPromptTemplateRequest;
import com.swygbro.airoad.backend.ai.domain.dto.request.UpdateAiPromptTemplateRequest;
import com.swygbro.airoad.backend.ai.domain.dto.response.AiPromptTemplateResponse;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.entity.AiPromptTemplate;
import com.swygbro.airoad.backend.ai.domain.entity.PromptType;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.ai.infrastructure.repository.AiPromptTemplateRepository;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.fixture.ai.AiPromptTemplateFixture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AiPromptTemplateCommandServiceTest {

  @Mock private AiPromptTemplateRepository aiPromptTemplateRepository;

  @InjectMocks private AiPromptTemplateCommandService aiPromptTemplateCommandService;

  @Nested
  @DisplayName("새로운 AI 프롬프트 템플릿을 생성 요청하면")
  class CreatePromptTemplate {

    @Test
    @DisplayName("프롬프트 템플릿이 생성되고 응답이 반환된다")
    void 프롬프트_템플릿이_생성되고_응답이_반환된다() {
      // given
      CreateAiPromptTemplateRequest request = AiPromptTemplateFixture.createPromptTemplateRequest();
      AiPromptTemplate savedTemplate = AiPromptTemplateFixture.createChatAgentSystemPrompt();

      given(aiPromptTemplateRepository.save(any(AiPromptTemplate.class))).willReturn(savedTemplate);

      // when
      AiPromptTemplateResponse response =
          aiPromptTemplateCommandService.createPromptTemplate(request);

      // then
      assertThat(response).isNotNull();
      verify(aiPromptTemplateRepository).save(any(AiPromptTemplate.class));
    }

    @Test
    @DisplayName("설명 없이 프롬프트 템플릿 생성이 가능하다")
    void 설명_없이_프롬프트_템플릿_생성이_가능하다() {
      // given
      CreateAiPromptTemplateRequest request =
          AiPromptTemplateFixture.createPromptTemplateRequestWithoutDescription();
      AiPromptTemplate savedTemplate = AiPromptTemplateFixture.createPromptWithoutDescription();

      given(aiPromptTemplateRepository.save(any(AiPromptTemplate.class))).willReturn(savedTemplate);

      // when
      AiPromptTemplateResponse response =
          aiPromptTemplateCommandService.createPromptTemplate(request);

      // then
      assertThat(response).isNotNull();
      verify(aiPromptTemplateRepository).save(any(AiPromptTemplate.class));
    }
  }

  @Nested
  @DisplayName("프롬프트 템플릿 수정을 요청하면")
  class UpdatePromptTemplate {

    @Test
    @DisplayName("존재하는 템플릿의 모든 필드가 수정된다")
    void 존재하는_템플릿의_모든_필드가_수정된다() {
      // given
      Long promptId = 1L;
      AiPromptTemplate template = AiPromptTemplateFixture.createChatAgentSystemPrompt();
      UpdateAiPromptTemplateRequest request = AiPromptTemplateFixture.updatePromptRequest();

      given(aiPromptTemplateRepository.findById(promptId)).willReturn(Optional.of(template));

      // when
      aiPromptTemplateCommandService.updatePromptTemplate(promptId, request);

      // then
      assertThat(template.getPrompt()).isEqualTo("수정된 프롬프트 내용");
      assertThat(template.getDescription()).isEqualTo("수정된 설명");
      assertThat(template.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("프롬프트 내용만 부분 수정이 가능하다")
    void 프롬프트_내용만_부분_수정이_가능하다() {
      // given
      Long promptId = 1L;
      AiPromptTemplate template = AiPromptTemplateFixture.createChatAgentSystemPrompt();
      UpdateAiPromptTemplateRequest request = AiPromptTemplateFixture.updatePromptOnlyRequest();
      String originalDescription = template.getDescription();
      Boolean originalIsActive = template.getIsActive();

      given(aiPromptTemplateRepository.findById(promptId)).willReturn(Optional.of(template));

      // when
      aiPromptTemplateCommandService.updatePromptTemplate(promptId, request);

      // then
      assertThat(template.getPrompt()).isEqualTo("프롬프트만 수정");
      assertThat(template.getDescription()).isEqualTo(originalDescription);
      assertThat(template.getIsActive()).isEqualTo(originalIsActive);
    }

    @Test
    @DisplayName("설명만 부분 수정이 가능하다")
    void 설명만_부분_수정이_가능하다() {
      // given
      Long promptId = 1L;
      AiPromptTemplate template = AiPromptTemplateFixture.createChatAgentSystemPrompt();
      UpdateAiPromptTemplateRequest request =
          AiPromptTemplateFixture.updateDescriptionOnlyRequest();
      String originalPrompt = template.getPrompt();
      Boolean originalIsActive = template.getIsActive();

      given(aiPromptTemplateRepository.findById(promptId)).willReturn(Optional.of(template));

      // when
      aiPromptTemplateCommandService.updatePromptTemplate(promptId, request);

      // then
      assertThat(template.getPrompt()).isEqualTo(originalPrompt);
      assertThat(template.getDescription()).isEqualTo("설명만 수정");
      assertThat(template.getIsActive()).isEqualTo(originalIsActive);
    }

    @Test
    @DisplayName("존재하지_않는_템플릿_수정_요청시_예외가_발생한다")
    void 존재하지_않는_템플릿_수정_요청시_예외가_발생한다() {
      // given
      Long promptId = 999L;
      UpdateAiPromptTemplateRequest request = AiPromptTemplateFixture.updatePromptRequest();

      given(aiPromptTemplateRepository.findById(promptId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(
              () -> aiPromptTemplateCommandService.updatePromptTemplate(promptId, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", AiErrorCode.TEMPLATE_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("프롬프트 템플릿 활성화를 요청하면")
  class ActivatePromptTemplate {

    @Test
    @DisplayName("동일한_타입의_다른_활성_템플릿이_비활성화되고_요청한_템플릿이_활성화된다")
    void 동일한_타입의_다른_활성_템플릿이_비활성화되고_요청한_템플릿이_활성화된다() {
      // given
      Long promptId = 2L;
      AiPromptTemplate currentlyActiveTemplate =
          AiPromptTemplateFixture.createChatAgentSystemPrompt();
      AiPromptTemplate targetTemplate = AiPromptTemplateFixture.createInactivePrompt();

      UpdateAiPromptTemplateRequest request =
          AiPromptTemplateFixture.updateIsActiveOnlyRequest(true);

      given(aiPromptTemplateRepository.findById(promptId)).willReturn(Optional.of(targetTemplate));
      given(aiPromptTemplateRepository.findByActivePrompt(PromptType.SYSTEM, AgentType.CHAT_AGENT))
          .willReturn(Optional.of(currentlyActiveTemplate));

      // when
      aiPromptTemplateCommandService.updatePromptTemplate(promptId, request);

      // then
      assertThat(currentlyActiveTemplate.getIsActive()).isFalse(); // 기존 활성 템플릿이 비활성화
      assertThat(targetTemplate.getIsActive()).isTrue(); // 요청한 템플릿이 활성화
    }

    @Test
    @DisplayName("동일한_타입의_활성_템플릿이_없으면_요청한_템플릿만_활성화된다")
    void 동일한_타입의_활성_템플릿이_없으면_요청한_템플릿만_활성화된다() {
      // given
      Long promptId = 1L;
      AiPromptTemplate targetTemplate = AiPromptTemplateFixture.createInactivePrompt();
      UpdateAiPromptTemplateRequest request =
          AiPromptTemplateFixture.updateIsActiveOnlyRequest(true);

      given(aiPromptTemplateRepository.findById(promptId)).willReturn(Optional.of(targetTemplate));
      given(aiPromptTemplateRepository.findByActivePrompt(PromptType.SYSTEM, AgentType.CHAT_AGENT))
          .willReturn(Optional.empty());

      // when
      aiPromptTemplateCommandService.updatePromptTemplate(promptId, request);

      // then
      assertThat(targetTemplate.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("이미_활성화된_템플릿을_다시_활성화_요청해도_상태가_유지된다")
    void 이미_활성화된_템플릿을_다시_활성화_요청해도_상태가_유지된다() {
      // given
      Long promptId = 1L;
      AiPromptTemplate alreadyActiveTemplate =
          AiPromptTemplateFixture.createChatAgentSystemPrompt();
      UpdateAiPromptTemplateRequest request =
          AiPromptTemplateFixture.updateIsActiveOnlyRequest(true);

      given(aiPromptTemplateRepository.findById(promptId))
          .willReturn(Optional.of(alreadyActiveTemplate));
      given(aiPromptTemplateRepository.findByActivePrompt(PromptType.SYSTEM, AgentType.CHAT_AGENT))
          .willReturn(Optional.of(alreadyActiveTemplate));

      // when
      aiPromptTemplateCommandService.updatePromptTemplate(promptId, request);

      // then
      assertThat(alreadyActiveTemplate.getIsActive()).isTrue();
    }
  }

  @Nested
  @DisplayName("프롬프트 템플릿 비활성화를 요청하면")
  class DeactivatePromptTemplate {

    @Test
    @DisplayName("활성화된_템플릿이_비활성화된다")
    void 활성화된_템플릿이_비활성화된다() {
      // given
      Long promptId = 1L;
      AiPromptTemplate activeTemplate = AiPromptTemplateFixture.createChatAgentSystemPrompt();
      UpdateAiPromptTemplateRequest request =
          AiPromptTemplateFixture.updateIsActiveOnlyRequest(false);

      given(aiPromptTemplateRepository.findById(promptId)).willReturn(Optional.of(activeTemplate));

      // when
      aiPromptTemplateCommandService.updatePromptTemplate(promptId, request);

      // then
      assertThat(activeTemplate.getIsActive()).isFalse();
    }
  }

  @Nested
  @DisplayName("프롬프트 템플릿 삭제를 요청하면")
  class DeletePromptTemplate {

    @Test
    @DisplayName("존재하는_템플릿이_삭제된다")
    void 존재하는_템플릿이_삭제된다() {
      // given
      Long promptId = 1L;

      given(aiPromptTemplateRepository.existsById(promptId)).willReturn(true);

      // when
      aiPromptTemplateCommandService.deletePromptTemplate(promptId);

      // then
      verify(aiPromptTemplateRepository).deleteById(promptId);
    }

    @Test
    @DisplayName("존재하지_않는_템플릿_삭제_요청시_예외가_발생한다")
    void 존재하지_않는_템플릿_삭제_요청시_예외가_발생한다() {
      // given
      Long promptId = 999L;

      given(aiPromptTemplateRepository.existsById(promptId)).willReturn(false);

      // when & then
      assertThatThrownBy(() -> aiPromptTemplateCommandService.deletePromptTemplate(promptId))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", AiErrorCode.TEMPLATE_NOT_FOUND);
    }
  }
}
