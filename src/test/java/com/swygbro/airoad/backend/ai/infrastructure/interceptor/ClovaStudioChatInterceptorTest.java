package com.swygbro.airoad.backend.ai.infrastructure.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

@ExtendWith(MockitoExtension.class)
class ClovaStudioChatInterceptorTest {

  private static final String CLOVA_CHAT_URL =
      "https://clovastudio.stream.ntruss.com/v1/openai/chat/completions";

  private ClovaStudioChatInterceptor interceptor;
  private ObjectMapper objectMapper;

  @Mock private HttpRequest request;
  @Mock private ClientHttpRequestExecution execution;
  @Mock private ClientHttpResponse clientHttpResponse;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    interceptor = new ClovaStudioChatInterceptor(objectMapper);
  }

  @Test
  @DisplayName("Clova Studio 챗 API 요청 시 max_completion_tokens가 1024로 설정되고 특정 필드가 제거되어야 한다")
  void shouldTransformChatRequestWhenTargetApiCalled() throws IOException {
    String originalBody =
        """
            {
              "model": "HCX-002",
              "messages": [{"role": "user", "content": "Hello"}],
              "temperature": 0.8,
              "$schema": "some-schema-info",
              "additionalProperties": {}
            }
            """;

    String resultBody = interceptAndGetBody(CLOVA_CHAT_URL, originalBody);
    JsonNode resultNode = objectMapper.readTree(resultBody);

    assertThat(resultNode.has("max_completion_tokens")).isTrue();
    assertThat(resultNode.get("max_completion_tokens").asInt()).isEqualTo(1024);
    assertThat(resultNode.has("$schema")).isFalse();
    assertThat(resultNode.has("additionalProperties")).isFalse();
    assertThat(resultNode.has("model")).isTrue();
    assertThat(resultNode.get("model").asText()).isEqualTo("HCX-003");
  }

  @Test
  @DisplayName("Clova Studio 챗 API가 아닌 경우 요청 본문은 변경되지 않아야 한다")
  void shouldKeepOriginalBodyWhenNotTargetApi() throws IOException {
    String originalBody =
        """
            {
              "model": "gpt-4",
              "messages": [{"role": "user", "content": "Hello"}],
              "temperature": 0.8,
              "$schema": "some-schema-info"
            }
            """;

    String resultBody = interceptAndGetBody("https://api.openai.com/v1/chat/completions", originalBody);
    assertThat(resultBody).isEqualTo(originalBody);
  }

  @Test
  @DisplayName("max_completion_tokens가 없는 경우 1024로 새로 추가되어야 한다")
  void shouldAddNewMaxCompletionTokensWhenMissing() throws IOException {
    String originalBody =
        """
            {
              "model": "HCX-002",
              "messages": [{"role": "user", "content": "Hello"}]
            }
            """;

    String resultBody = interceptAndGetBody(CLOVA_CHAT_URL, originalBody);
    JsonNode resultNode = objectMapper.readTree(resultBody);

    assertThat(resultNode.has("max_completion_tokens")).isTrue();
    assertThat(resultNode.get("max_completion_tokens").asInt()).isEqualTo(1024);
  }

  @Test
  @DisplayName("max_completion_tokens가 기존에 있어도 1024로 덮어씌워져야 한다")
  void shouldOverwriteExistingMaxCompletionTokens() throws IOException {
    String originalBody =
        """
            {
              "model": "HCX-002",
              "messages": [{"role": "user", "content": "Hello"}],
              "max_completion_tokens": 500
            }
            """;

    String resultBody = interceptAndGetBody(CLOVA_CHAT_URL, originalBody);
    JsonNode resultNode = objectMapper.readTree(resultBody);

    assertThat(resultNode.has("max_completion_tokens")).isTrue();
    assertThat(resultNode.get("max_completion_tokens").asInt()).isEqualTo(1024);
  }

  @Test
  @DisplayName("$schema와 additionalProperties 필드가 중첩된 구조에서도 재귀적으로 제거되어야 한다")
  void shouldRecursivelyRemoveSpecificFields() throws IOException {
    String originalBody =
        """
            {
              "model": "HCX-002",
              "$schema": "root-schema",
              "messages": [
                {
                  "role": "user",
                  "content": "Hello",
                  "$schema": "message-schema"
                },
                {
                  "role": "assistant",
                  "content": {
                    "text": "Hi",
                    "additionalProperties": {"key": "value"}
                  }
                }
              ],
              "config": {
                "param": 1,
                "additionalProperties": {},
                "nested": {
                  "$schema": "nested-schema"
                }
              }
            }
            """;

    String resultBody = interceptAndGetBody(CLOVA_CHAT_URL, originalBody);
    JsonNode resultNode = objectMapper.readTree(resultBody);

    assertThat(resultNode.has("$schema")).isFalse();
    assertThat(resultNode.has("additionalProperties")).isFalse();
    assertThat(resultNode.at("/messages/0").has("$schema")).isFalse();
    assertThat(resultNode.at("/messages/1/content").has("additionalProperties")).isFalse();
    assertThat(resultNode.at("/config").has("additionalProperties")).isFalse();
    assertThat(resultNode.at("/config/nested").has("$schema")).isFalse();
    assertThat(resultNode.at("/config/param").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("입력 JSON 형식이 올바르지 않은 경우 IOException을 발생시켜야 한다")
  void shouldThrowIOExceptionWhenJsonIsMalformed() throws IOException {
    String malformedBody = "{invalid json}";
    URI targetUri = URI.create(CLOVA_CHAT_URL);

    given(request.getURI()).willReturn(targetUri);

    assertThatThrownBy(() -> interceptor.intercept(request, malformedBody.getBytes(StandardCharsets.UTF_8), execution))
        .isInstanceOf(IOException.class);
  }

  private String interceptAndGetBody(String url, String content) throws IOException {
    given(request.getURI()).willReturn(URI.create(url));
    given(execution.execute(any(HttpRequest.class), any(byte[].class)))
        .willReturn(clientHttpResponse);

    interceptor.intercept(request, content.getBytes(StandardCharsets.UTF_8), execution);

    ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(execution).execute(any(HttpRequest.class), bodyCaptor.capture());

    return new String(bodyCaptor.getValue(), StandardCharsets.UTF_8);
  }
}
