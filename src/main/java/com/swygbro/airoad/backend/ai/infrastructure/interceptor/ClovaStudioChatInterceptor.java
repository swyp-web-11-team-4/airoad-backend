package com.swygbro.airoad.backend.ai.infrastructure.interceptor;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClovaStudioChatInterceptor implements ClientHttpRequestInterceptor {

  private static final String CLOVA_STUDIO_CHAT_API_URL =
      "https://clovastudio.stream.ntruss.com/v1/openai/chat/completions";

  private final ObjectMapper objectMapper;

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    log.debug("request header: {}", request.getHeaders());
    log.debug("request body: {}", new String(body));

    String uri = request.getURI().toString();

    if (uri.equals(CLOVA_STUDIO_CHAT_API_URL)) {
      String json = new String(body);
      body = convertField(json);
    }

    return execution.execute(request, body);
  }

  private byte[] convertField(String json) throws JsonProcessingException {
    JsonNode rootNode = objectMapper.readTree(json);

    if (rootNode.isObject()) {
      ((ObjectNode) rootNode).put("max_completion_tokens", 1024);
    }

    removeFieldRecursively(rootNode, "$schema");
    removeFieldRecursively(rootNode, "additionalProperties");

    return objectMapper.writeValueAsBytes(rootNode);
  }

  private void removeFieldRecursively(JsonNode node, String fieldName) {
    if (node.isObject()) {
      ObjectNode objectNode = (ObjectNode) node;
      objectNode.remove(fieldName);
      objectNode.elements().forEachRemaining(child -> removeFieldRecursively(child, fieldName));
    } else if (node.isArray()) {
      for (JsonNode element : node) {
        removeFieldRecursively(element, fieldName);
      }
    }
  }
}
