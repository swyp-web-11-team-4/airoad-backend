package com.swygbro.airoad.backend.example.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.example.application.ExampleUseCase;
import com.swygbro.airoad.backend.example.domain.dto.ExampleResponse;
import com.swygbro.airoad.backend.example.exception.ExampleErrorCode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** ExampleController의 테스트 클래스입니다. */
@WebMvcTest(ExampleController.class)
@ActiveProfiles("test")
class ExampleControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ExampleUseCase exampleUseCase;

  @Nested
  @DisplayName("GET /api/v1/examples/hello")
  class GetHello {

    @Test
    @DisplayName("Hello World 메시지를 반환한다")
    void shouldReturnHelloMessage() throws Exception {
      // given
      String expectedMessage = "Hello, World!";
      given(exampleUseCase.getHelloMessage()).willReturn(ExampleResponse.of(expectedMessage));

      // when & then
      mockMvc
          .perform(get("/api/v1/examples/hello"))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(200))
          .andExpect(jsonPath("$.data.name").value(expectedMessage));
    }
  }

  @Nested
  @DisplayName("GET /api/v1/examples/{id}")
  class GetExampleById {

    @Test
    @DisplayName("ID로 Example을 조회하여 반환한다")
    void shouldReturnExampleById() throws Exception {
      // given
      Long exampleId = 1L;
      ExampleResponse response = new ExampleResponse(exampleId, "Test Example");
      given(exampleUseCase.getExampleById(exampleId)).willReturn(response);

      // when & then
      mockMvc
          .perform(get("/api/v1/examples/{id}", exampleId))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(200))
          .andExpect(jsonPath("$.data.id").value(exampleId))
          .andExpect(jsonPath("$.data.name").value("Test Example"));
    }

    @Test
    @DisplayName("존재하지 않는 Example 조회 시 404 에러를 반환한다")
    void shouldReturn404WhenExampleNotFound() throws Exception {
      // given
      Long nonExistentId = 999L;
      given(exampleUseCase.getExampleById(anyLong()))
          .willThrow(new BusinessException(ExampleErrorCode.EXAMPLE_NOT_FOUND));

      // when & then
      mockMvc.perform(get("/api/v1/examples/{id}", nonExistentId)).andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("POST /api/v1/examples")
  class CreateExample {

    @Test
    @DisplayName("유효한 이름으로 Example을 생성한다")
    void shouldCreateExampleWithValidName() throws Exception {
      // given
      String name = "New Example";
      ExampleResponse response = new ExampleResponse(1L, name);
      given(exampleUseCase.createExample(name)).willReturn(response);

      // when & then
      mockMvc
          .perform(post("/api/v1/examples").param("name", name))
          .andExpect(status().isCreated())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(201))
          .andExpect(jsonPath("$.data.id").value(1L))
          .andExpect(jsonPath("$.data.name").value(name));
    }

    @Test
    @DisplayName("유효하지 않은 이름으로 생성 시 400 에러를 반환한다")
    void shouldReturn400WhenNameIsInvalid() throws Exception {
      // given
      String invalidName = "   ";
      given(exampleUseCase.createExample(any()))
          .willThrow(new BusinessException(ExampleErrorCode.INVALID_EXAMPLE_NAME));

      // when & then
      mockMvc
          .perform(post("/api/v1/examples").param("name", invalidName))
          .andExpect(status().isBadRequest());
    }
  }
}
