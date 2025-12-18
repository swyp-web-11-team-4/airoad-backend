package com.swygbro.airoad.backend.ai.infrastructure.interceptor;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClovaStudioEmbeddingInterceptorTest {

  private static final String CLOVA_EMBEDDING_URL =
      "https://clovastudio.stream.ntruss.com/v1/openai/embeddings";

  private ClovaStudioEmbeddingInterceptor interceptor;

  @Mock private HttpRequest request;
  @Mock private ClientHttpRequestExecution execution;

  @BeforeEach
  void setUp() {
    interceptor = new ClovaStudioEmbeddingInterceptor(new ObjectMapper());
  }

  @Test
  @DisplayName("Clova Studio 임베딩 API 요청 시 입력값이 단일 항목 배열이면 단일 문자열로 변환되어야 한다")
  void shouldConvertArrayInputToStringWhenRequestingClovaEmbeddingApi() throws IOException {
    String originalBody = "{\"input\": [\"test input\"]}";

    String resultBody = interceptAndGetBody(CLOVA_EMBEDDING_URL, originalBody);

    assertThat(resultBody).contains("\"input\":\"test input\"");
    assertThat(resultBody).doesNotContain("[");
  }

  @Test
  @DisplayName("Clova Studio 임베딩 API가 아닌 경우 요청 본문은 변경되지 않아야 한다")
  void shouldKeepOriginalBodyWhenRequestingOtherApi() throws IOException {
    String originalBody = "{\"input\": [\"test input\"]}";

    String resultBody =
        interceptAndGetBody("https://api.other-service.com/v1/embeddings", originalBody);

    assertThat(resultBody).isEqualTo(originalBody);
  }

  @Test
  @DisplayName("입력값이 여러 개인 경우 변환하지 않고 원본을 유지해야 한다")
  void shouldKeepOriginalBodyWhenInputArrayHasMultipleItems() throws IOException {
    String originalBody = "{\"input\": [\"item1\", \"item2\"]}";

    String resultBody = interceptAndGetBody(CLOVA_EMBEDDING_URL, originalBody);

    assertThat(resultBody).isEqualTo(originalBody);
  }

  @Test
  @DisplayName("입력 JSON 형식이 올바르지 않은 경우 예외를 발생시키지 않고 원본을 유지해야 한다")
  void shouldKeepOriginalBodyWhenJsonIsMalformed() throws IOException {
    String malformedBody = "{invalid json}";

    String resultBody = interceptAndGetBody(CLOVA_EMBEDDING_URL, malformedBody);

    assertThat(resultBody).isEqualTo(malformedBody);
  }

  private String interceptAndGetBody(String url, String content) throws IOException {
    given(request.getURI()).willReturn(URI.create(url));

    interceptor.intercept(request, content.getBytes(StandardCharsets.UTF_8), execution);

    ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(execution).execute(any(HttpRequest.class), bodyCaptor.capture());

    return new String(bodyCaptor.getValue(), StandardCharsets.UTF_8);
  }
}
