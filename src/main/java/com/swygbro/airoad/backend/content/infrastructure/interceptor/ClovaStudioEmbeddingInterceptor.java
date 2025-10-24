package com.swygbro.airoad.backend.content.infrastructure.interceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Clova Studio Embedding API 전용 인터셉터
 *
 * <p>Spring AI는 임베딩 요청 시 input을 항상 배열로 전송하지만, Clova Studio OpenAI 호환 API는 배열 형식을 제대로 처리하지 못합니다. 이
 * 인터셉터는 embedding API 요청의 input 배열을 문자열로 변환합니다.
 */
@Slf4j
@Component
public class ClovaStudioEmbeddingInterceptor implements ClientHttpRequestInterceptor {

  private static final String CLOVA_STUDIO_EMBEDDING_API_URL =
      "https://clovastudio.stream.ntruss.com/v1/openai/embeddings";

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    String uri = request.getURI().toString();
    String bodyStr = new String(body, StandardCharsets.UTF_8);

    if (uri.equals(CLOVA_STUDIO_EMBEDDING_API_URL) && bodyStr.contains("\"input\":[")) {

      log.debug("=== Clova Studio Embedding Request Modification ===");
      log.debug("Original Body: {}", bodyStr);

      String modifiedBody = bodyStr.replaceFirst("\"input\":\\[\"([^\"]+)\"]", "\"input\":\"$1\"");

      log.debug("Modified Body: {}", modifiedBody);

      body = modifiedBody.getBytes(StandardCharsets.UTF_8);
    }

    return execution.execute(request, body);
  }
}
