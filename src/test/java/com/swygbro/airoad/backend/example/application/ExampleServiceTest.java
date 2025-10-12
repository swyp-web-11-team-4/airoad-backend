package com.swygbro.airoad.backend.example.application;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.example.domain.dto.ExampleResponse;
import com.swygbro.airoad.backend.example.domain.entity.Example;
import com.swygbro.airoad.backend.example.exception.ExampleErrorCode;
import com.swygbro.airoad.backend.example.infrastructure.ExampleRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/** ExampleService의 테스트 클래스입니다. */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ExampleServiceTest {

  @InjectMocks private ExampleService exampleService;

  @Mock private ExampleRepository exampleRepository;

  @Nested
  @DisplayName("getHelloMessage 메서드는")
  class GetHelloMessage {

    @Test
    @DisplayName("Hello World 메시지를 반환한다")
    void shouldReturnHelloWorldMessage() {
      // given
      String expectedMessage = "Hello, World!";

      // when
      ExampleResponse response = exampleService.getHelloMessage();

      // then
      assertThat(response).isNotNull();
      assertThat(response.name()).isEqualTo(expectedMessage);
      assertThat(response.id()).isNull();
    }
  }

  @Nested
  @DisplayName("getExampleById 메서드는")
  class GetExampleById {

    @Test
    @DisplayName("ID로 Example을 조회하여 반환한다")
    void shouldReturnExampleById() {
      // given
      Long exampleId = 1L;
      Example example = new Example(exampleId, "Test Example");
      given(exampleRepository.findById(exampleId)).willReturn(Optional.of(example));

      // when
      ExampleResponse response = exampleService.getExampleById(exampleId);

      // then
      assertThat(response).isNotNull();
      assertThat(response.id()).isEqualTo(exampleId);
      assertThat(response.name()).isEqualTo("Test Example");
      verify(exampleRepository).findById(exampleId);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 BusinessException을 던진다")
    void shouldThrowBusinessExceptionWhenExampleNotFound() {
      // given
      Long nonExistentId = 999L;
      given(exampleRepository.findById(nonExistentId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> exampleService.getExampleById(nonExistentId))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ExampleErrorCode.EXAMPLE_NOT_FOUND.getDefaultMessage())
          .extracting(e -> ((BusinessException) e).getErrorCode())
          .isEqualTo(ExampleErrorCode.EXAMPLE_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("createExample 메서드는")
  class CreateExample {

    @Test
    @DisplayName("유효한 이름으로 Example을 생성한다")
    void shouldCreateExampleWithValidName() {
      // given
      String name = "New Example";
      Example savedExample = new Example(1L, name);
      given(exampleRepository.save(any(Example.class))).willReturn(savedExample);

      // when
      ExampleResponse response = exampleService.createExample(name);

      // then
      assertThat(response).isNotNull();
      assertThat(response.id()).isEqualTo(1L);
      assertThat(response.name()).isEqualTo(name);
      verify(exampleRepository).save(any(Example.class));
    }

    @Test
    @DisplayName("null 이름으로 생성 시 BusinessException을 던진다")
    void shouldThrowBusinessExceptionWhenNameIsNull() {
      // given
      String nullName = null;

      // when & then
      assertThatThrownBy(() -> exampleService.createExample(nullName))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ExampleErrorCode.INVALID_EXAMPLE_NAME.getDefaultMessage())
          .extracting(e -> ((BusinessException) e).getErrorCode())
          .isEqualTo(ExampleErrorCode.INVALID_EXAMPLE_NAME);
    }

    @Test
    @DisplayName("빈 문자열 이름으로 생성 시 BusinessException을 던진다")
    void shouldThrowBusinessExceptionWhenNameIsEmpty() {
      // given
      String emptyName = "   ";

      // when & then
      assertThatThrownBy(() -> exampleService.createExample(emptyName))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ExampleErrorCode.INVALID_EXAMPLE_NAME.getDefaultMessage())
          .extracting(e -> ((BusinessException) e).getErrorCode())
          .isEqualTo(ExampleErrorCode.INVALID_EXAMPLE_NAME);
    }
  }
}
