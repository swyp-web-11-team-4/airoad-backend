package com.swygbro.airoad.backend.ai.infrastructure.interceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Clova Studio Embedding API 전용 인터셉터
 *
 * <p>Spring AI는 임베딩 요청 시 input을 항상 배열로 전송하지만, Clova Studio OpenAI 호환 API는 배열 형식을 제대로 처리하지 못합니다. 이
 * 인터셉터는 embedding API 요청의 input 배열을 배열 형식이 아닌 단일 문자열로 변환합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClovaStudioEmbeddingInterceptor implements ClientHttpRequestInterceptor {

  private static final String CLOVA_STUDIO_EMBEDDING_API_URL =
      "https://clovastudio.stream.ntruss.com/v1/openai/embeddings";

  private final ObjectMapper objectMapper;

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    String uri = request.getURI().toString();

    if (uri.equals(CLOVA_STUDIO_EMBEDDING_API_URL)) {
      body = transformInputArrayToString(body);
    }

    return execution.execute(request, body);
  }

  /**
   * input 배열을 문자열로 변환
   *
   * @param body 원본 요청 body
   * @return 변환된 요청 body
   * @throws IOException JSON 파싱 실패 시
   */
  private byte[] transformInputArrayToString(byte[] body) throws IOException {
    String bodyStr = new String(body, StandardCharsets.UTF_8);

    try {
      JsonNode rootNode = objectMapper.readTree(bodyStr);

      if (rootNode.has("input") && rootNode.get("input").isArray()) {
        JsonNode inputArray = rootNode.get("input");

        if (inputArray.size() == 1) {
          String singleValue = inputArray.get(0).asText();
          ((ObjectNode) rootNode).put("input", singleValue);
          String modifiedBody = objectMapper.writeValueAsString(rootNode);

          return modifiedBody.getBytes(StandardCharsets.UTF_8);
        } else if (inputArray.size() > 1) {
          log.warn("CLOVA X 임베딩 요청 입력 값에 여러 항목이 존재해 API 요청에 실패할 수 있습니다. ({}).", inputArray.size());
        }
      }
    } catch (Exception e) {
      log.error("CLOVA X 임베딩 API 요청 본문을 파싱하는 데 실패했습니다.", e);
    }

    return body;
  }
}
