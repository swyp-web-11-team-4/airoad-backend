package com.swygbro.airoad.backend.ai.application.query;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.ai.domain.dto.response.AiPromptTemplateResponse;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.entity.AiPromptTemplate;
import com.swygbro.airoad.backend.ai.domain.entity.PromptType;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.ai.infrastructure.repository.AiPromptTemplateRepository;
import com.swygbro.airoad.backend.common.domain.dto.PageResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.fixture.ai.AiPromptTemplateFixture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AiPromptTemplateQueryServiceTest {

  @Mock private AiPromptTemplateRepository aiPromptTemplateRepository;

  @InjectMocks private AiPromptTemplateQueryService aiPromptTemplateQueryService;

  @Nested
  @DisplayName("프롬프트 템플릿 목록 조회를 요청하면")
  class FindPromptTemplates {

    @Test
    @DisplayName("페이지네이션된_프롬프트_템플릿_목록이_반환된다")
    void 페이지네이션된_프롬프트_템플릿_목록이_반환된다() {
      // given
      int page = 0;
      int size = 10;
      String sort = "createdAt";
      String order = "desc";

      List<AiPromptTemplate> templates =
          List.of(
              AiPromptTemplateFixture.createChatAgentSystemPrompt(),
              AiPromptTemplateFixture.createTripAgentSystemPrompt(),
              AiPromptTemplateFixture.createPlaceSummaryAgentSystemPrompt());

      PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.DESC, sort);
      Page<AiPromptTemplate> templatePage =
          new PageImpl<>(templates, pageRequest, templates.size());

      given(aiPromptTemplateRepository.findAll(pageRequest)).willReturn(templatePage);

      // when
      PageResponse<AiPromptTemplateResponse> response =
          aiPromptTemplateQueryService.findPromptTemplates(page, size, sort, order);

      // then
      assertThat(response.content()).hasSize(3);
      assertThat(response.totalElements()).isEqualTo(3);
      assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("빈_페이지를_요청하면_빈_목록이_반환된다")
    void 빈_페이지를_요청하면_빈_목록이_반환된다() {
      // given
      int page = 0;
      int size = 10;
      String sort = "createdAt";
      String order = "asc";

      PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.ASC, sort);
      Page<AiPromptTemplate> emptyPage = Page.empty(pageRequest);

      given(aiPromptTemplateRepository.findAll(pageRequest)).willReturn(emptyPage);

      // when
      PageResponse<AiPromptTemplateResponse> response =
          aiPromptTemplateQueryService.findPromptTemplates(page, size, sort, order);

      // then
      assertThat(response.content()).isEmpty();
      assertThat(response.totalElements()).isZero();
    }

    @Test
    @DisplayName("정렬_방향을_오름차순으로_요청할_수_있다")
    void 정렬_방향을_오름차순으로_요청할_수_있다() {
      // given
      int page = 0;
      int size = 10;
      String sort = "id";
      String order = "asc";

      List<AiPromptTemplate> templates =
          List.of(AiPromptTemplateFixture.createChatAgentSystemPrompt());

      PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.ASC, sort);
      Page<AiPromptTemplate> templatePage =
          new PageImpl<>(templates, pageRequest, templates.size());

      given(aiPromptTemplateRepository.findAll(any(PageRequest.class))).willReturn(templatePage);

      // when
      PageResponse<AiPromptTemplateResponse> response =
          aiPromptTemplateQueryService.findPromptTemplates(page, size, sort, order);

      // then
      assertThat(response).isNotNull();
      assertThat(response.content()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("특정_ID로_프롬프트_템플릿_조회를_요청하면")
  class FindPromptTemplate {

    @Test
    @DisplayName("존재하는_템플릿이_반환된다")
    void 존재하는_템플릿이_반환된다() {
      // given
      Long promptId = 1L;
      AiPromptTemplate template = AiPromptTemplateFixture.createChatAgentSystemPrompt();

      given(aiPromptTemplateRepository.findById(promptId)).willReturn(Optional.of(template));

      // when
      AiPromptTemplateResponse response = aiPromptTemplateQueryService.findPromptTemplate(promptId);

      // then
      assertThat(response).isNotNull();
      assertThat(response.promptType()).isEqualTo(PromptType.SYSTEM);
      assertThat(response.agentType()).isEqualTo(AgentType.CHAT_AGENT);
    }

    @Test
    @DisplayName("존재하지_않는_템플릿_조회시_예외가_발생한다")
    void 존재하지_않는_템플릿_조회시_예외가_발생한다() {
      // given
      Long promptId = 999L;

      given(aiPromptTemplateRepository.findById(promptId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> aiPromptTemplateQueryService.findPromptTemplate(promptId))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", AiErrorCode.TEMPLATE_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("활성화된_프롬프트_템플릿_조회를_요청하면")
  class FindActivePromptTemplate {

    @Test
    @DisplayName("특정_에이전트_타입과_프롬프트_타입에_해당하는_활성_템플릿이_반환된다")
    void 특정_에이전트_타입과_프롬프트_타입에_해당하는_활성_템플릿이_반환된다() {
      // given
      PromptType promptType = PromptType.SYSTEM;
      AgentType agentType = AgentType.CHAT_AGENT;
      AiPromptTemplate activeTemplate = AiPromptTemplateFixture.createChatAgentSystemPrompt();

      given(aiPromptTemplateRepository.findByActivePrompt(promptType, agentType))
          .willReturn(Optional.of(activeTemplate));

      // when
      AiPromptTemplateResponse response =
          aiPromptTemplateQueryService.findActivePromptTemplate(promptType, agentType);

      // then
      assertThat(response).isNotNull();
      assertThat(response.promptType()).isEqualTo(PromptType.SYSTEM);
      assertThat(response.agentType()).isEqualTo(AgentType.CHAT_AGENT);
      assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("TripAgent의_USER_프롬프트를_조회할_수_있다")
    void TripAgent의_USER_프롬프트를_조회할_수_있다() {
      // given
      PromptType promptType = PromptType.USER;
      AgentType agentType = AgentType.TRIP_AGENT;
      AiPromptTemplate activeTemplate = AiPromptTemplateFixture.createTripAgentUserPrompt();

      given(aiPromptTemplateRepository.findByActivePrompt(promptType, agentType))
          .willReturn(Optional.of(activeTemplate));

      // when
      AiPromptTemplateResponse response =
          aiPromptTemplateQueryService.findActivePromptTemplate(promptType, agentType);

      // then
      assertThat(response).isNotNull();
      assertThat(response.promptType()).isEqualTo(PromptType.USER);
      assertThat(response.agentType()).isEqualTo(AgentType.TRIP_AGENT);
    }

    @Test
    @DisplayName("활성화된_템플릿이_없으면_예외가_발생한다")
    void 활성화된_템플릿이_없으면_예외가_발생한다() {
      // given
      PromptType promptType = PromptType.SYSTEM;
      AgentType agentType = AgentType.CHAT_AGENT;

      given(aiPromptTemplateRepository.findByActivePrompt(promptType, agentType))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(
              () -> aiPromptTemplateQueryService.findActivePromptTemplate(promptType, agentType))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", AiErrorCode.TEMPLATE_NOT_FOUND);
    }
  }
}
