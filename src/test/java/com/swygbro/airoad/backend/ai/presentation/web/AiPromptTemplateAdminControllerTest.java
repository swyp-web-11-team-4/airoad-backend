package com.swygbro.airoad.backend.ai.presentation.web;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swygbro.airoad.backend.ai.application.command.AiPromptTemplateCommandUseCase;
import com.swygbro.airoad.backend.ai.application.query.AiPromptTemplateQueryUseCase;
import com.swygbro.airoad.backend.ai.domain.dto.request.CreateAiPromptTemplateRequest;
import com.swygbro.airoad.backend.ai.domain.dto.request.UpdateAiPromptTemplateRequest;
import com.swygbro.airoad.backend.ai.domain.dto.response.AiPromptTemplateResponse;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.ai.domain.entity.AiPromptTemplate;
import com.swygbro.airoad.backend.ai.domain.entity.PromptType;
import com.swygbro.airoad.backend.ai.exception.AiErrorCode;
import com.swygbro.airoad.backend.common.domain.dto.PageResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.common.presentation.GlobalExceptionHandler;
import com.swygbro.airoad.backend.fixture.ai.AiPromptTemplateFixture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AiPromptTemplateAdminControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock private AiPromptTemplateCommandUseCase aiPromptTemplateCommandUseCase;

  @Mock private AiPromptTemplateQueryUseCase aiPromptTemplateQueryUseCase;

  @InjectMocks private AiPromptTemplateAdminController aiPromptTemplateAdminController;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
        MockMvcBuilders.standaloneSetup(aiPromptTemplateAdminController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Nested
  @DisplayName("프롬프트_템플릿_목록_조회를_요청하면")
  class GetPromptTemplates {

    @Test
    @DisplayName("페이지네이션된_프롬프트_템플릿_목록이_반환된다")
    void 페이지네이션된_프롬프트_템플릿_목록이_반환된다() throws Exception {
      // given
      List<AiPromptTemplate> templates =
          List.of(
              AiPromptTemplateFixture.createChatAgentSystemPrompt(),
              AiPromptTemplateFixture.createTripAgentSystemPrompt());

      Page<AiPromptTemplateResponse> page =
          new PageImpl<>(
              templates.stream().map(AiPromptTemplateResponse::of).toList(),
              PageRequest.of(0, 10),
              templates.size());
      PageResponse<AiPromptTemplateResponse> pageResponse = PageResponse.of(page);

      given(aiPromptTemplateQueryUseCase.findPromptTemplates(0, 10, "createdAt", "desc"))
          .willReturn(pageResponse);

      // when & then
      mockMvc
          .perform(
              get("/api/v1/admin/prompts")
                  .param("page", "0")
                  .param("size", "10")
                  .param("sort", "createdAt")
                  .param("order", "desc"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(200))
          .andExpect(jsonPath("$.data.content").isArray())
          .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("페이지_파라미터_없이_요청하면_기본값으로_조회된다")
    void 페이지_파라미터_없이_요청하면_기본값으로_조회된다() throws Exception {
      // given
      PageResponse<AiPromptTemplateResponse> emptyPage =
          PageResponse.of(Page.empty(PageRequest.of(0, 10)));

      given(aiPromptTemplateQueryUseCase.findPromptTemplates(0, 10, "createdAt", "desc"))
          .willReturn(emptyPage);

      // when & then
      mockMvc
          .perform(get("/api/v1/admin/prompts"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(200));
    }
  }

  @Nested
  @DisplayName("특정_프롬프트_템플릿_조회를_요청하면")
  class GetPromptTemplate {

    @Test
    @DisplayName("존재하는_템플릿_정보가_반환된다")
    void 존재하는_템플릿_정보가_반환된다() throws Exception {
      // given
      Long promptId = 1L;
      AiPromptTemplate template = AiPromptTemplateFixture.createChatAgentSystemPrompt();
      AiPromptTemplateResponse response = AiPromptTemplateResponse.of(template);

      given(aiPromptTemplateQueryUseCase.findPromptTemplate(promptId)).willReturn(response);

      // when & then
      mockMvc
          .perform(get("/api/v1/admin/prompts/{promptId}", promptId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(200))
          .andExpect(jsonPath("$.data.promptType").value("SYSTEM"))
          .andExpect(jsonPath("$.data.agentType").value("CHAT_AGENT"));
    }

    @Test
    @DisplayName("존재하지_않는_템플릿_조회시_404_에러가_반환된다")
    void 존재하지_않는_템플릿_조회시_404_에러가_반환된다() throws Exception {
      // given
      Long promptId = 999L;

      given(aiPromptTemplateQueryUseCase.findPromptTemplate(promptId))
          .willThrow(new BusinessException(AiErrorCode.TEMPLATE_NOT_FOUND));

      // when & then
      mockMvc
          .perform(get("/api/v1/admin/prompts/{promptId}", promptId))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("새로운_프롬프트_템플릿_생성을_요청하면")
  class CreatePromptTemplate {

    @Test
    @DisplayName("템플릿이_생성되고_201_Created_응답이_반환된다")
    void 템플릿이_생성되고_201_Created_응답이_반환된다() throws Exception {
      // given
      CreateAiPromptTemplateRequest request =
          CreateAiPromptTemplateRequest.builder()
              .promptType(PromptType.SYSTEM)
              .agentType(AgentType.CHAT_AGENT)
              .prompt("새로운 프롬프트")
              .description("새로운 설명")
              .build();

      AiPromptTemplate createdTemplate = AiPromptTemplateFixture.createChatAgentSystemPrompt();
      AiPromptTemplateResponse response = AiPromptTemplateResponse.of(createdTemplate);

      given(
              aiPromptTemplateCommandUseCase.createPromptTemplate(
                  any(CreateAiPromptTemplateRequest.class)))
          .willReturn(response);

      // when & then
      mockMvc
          .perform(
              post("/api/v1/admin/prompts")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(201))
          .andExpect(jsonPath("$.data.promptType").value("SYSTEM"))
          .andExpect(jsonPath("$.data.agentType").value("CHAT_AGENT"));
    }
  }

  @Nested
  @DisplayName("프롬프트_템플릿_수정을_요청하면")
  class UpdatePromptTemplate {

    @Test
    @DisplayName("템플릿이_수정되고_200_OK_응답이_반환된다")
    void 템플릿이_수정되고_200_OK_응답이_반환된다() throws Exception {
      // given
      Long promptId = 1L;
      UpdateAiPromptTemplateRequest request =
          UpdateAiPromptTemplateRequest.builder()
              .prompt("수정된 프롬프트")
              .isActive(true)
              .description("수정된 설명")
              .build();

      willDoNothing()
          .given(aiPromptTemplateCommandUseCase)
          .updatePromptTemplate(eq(promptId), any(UpdateAiPromptTemplateRequest.class));

      // when & then
      mockMvc
          .perform(
              patch("/api/v1/admin/prompts/{promptId}", promptId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("부분_수정_요청이_가능하다")
    void 부분_수정_요청이_가능하다() throws Exception {
      // given
      Long promptId = 1L;
      UpdateAiPromptTemplateRequest request =
          UpdateAiPromptTemplateRequest.builder().prompt("프롬프트만 수정").build();

      willDoNothing()
          .given(aiPromptTemplateCommandUseCase)
          .updatePromptTemplate(eq(promptId), any(UpdateAiPromptTemplateRequest.class));

      // when & then
      mockMvc
          .perform(
              patch("/api/v1/admin/prompts/{promptId}", promptId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("존재하지_않는_템플릿_수정시_404_에러가_반환된다")
    void 존재하지_않는_템플릿_수정시_404_에러가_반환된다() throws Exception {
      // given
      Long promptId = 999L;
      UpdateAiPromptTemplateRequest request = AiPromptTemplateFixture.updatePromptRequest();

      willThrow(new BusinessException(AiErrorCode.TEMPLATE_NOT_FOUND))
          .given(aiPromptTemplateCommandUseCase)
          .updatePromptTemplate(eq(promptId), any(UpdateAiPromptTemplateRequest.class));

      // when & then
      mockMvc
          .perform(
              patch("/api/v1/admin/prompts/{promptId}", promptId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("프롬프트_템플릿_삭제를_요청하면")
  class DeletePromptTemplate {

    @Test
    @DisplayName("템플릿이_삭제되고_204_No_Content_응답이_반환된다")
    void 템플릿이_삭제되고_204_No_Content_응답이_반환된다() throws Exception {
      // given
      Long promptId = 1L;

      willDoNothing().given(aiPromptTemplateCommandUseCase).deletePromptTemplate(promptId);

      // when & then
      mockMvc
          .perform(delete("/api/v1/admin/prompts/{promptId}", promptId))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("존재하지_않는_템플릿_삭제시_404_에러가_반환된다")
    void 존재하지_않는_템플릿_삭제시_404_에러가_반환된다() throws Exception {
      // given
      Long promptId = 999L;

      willThrow(new BusinessException(AiErrorCode.TEMPLATE_NOT_FOUND))
          .given(aiPromptTemplateCommandUseCase)
          .deletePromptTemplate(promptId);

      // when & then
      mockMvc
          .perform(delete("/api/v1/admin/prompts/{promptId}", promptId))
          .andExpect(status().isNotFound());
    }
  }
}
