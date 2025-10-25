package com.swygbro.airoad.backend.content.infrastructure.interceptor;

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

  private final ObjectMapper objectMapper = new ObjectMapper();

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
   * <p>Spring AI는 input을 배열로 전송하지만, Clova Studio는 단일 문자열을 요구합니다. 안전한 JSON 파싱을 통해 단일 항목 배열만 문자열로
   * 변환합니다.
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
          // 단일 항목 배열인 경우에만 문자열로 변환
          String singleValue = inputArray.get(0).asText();
          ((ObjectNode) rootNode).put("input", singleValue);

          String modifiedBody = objectMapper.writeValueAsString(rootNode);
          log.debug("=== Clova Studio Embedding Request Modification ===");
          log.debug("Transformed input array to string: {}", singleValue);

          return modifiedBody.getBytes(StandardCharsets.UTF_8);
        } else if (inputArray.size() > 1) {
          // 다중 항목 배열은 경고 후 원본 유지
          log.warn(
              "Multiple items in input array ({}). Clova Studio expects single string. Request may fail.",
              inputArray.size());
        }
      }
    } catch (Exception e) {
      log.error("Failed to parse request body for input transformation", e);
      // 파싱 실패 시 원본 반환
    }

    return body;
  }
}
