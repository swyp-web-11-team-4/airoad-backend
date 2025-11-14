package com.swygbro.airoad.backend.ai.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class NaverChatModelConfig {

  @Value("${spring.ai.naver.api-key}")
  private String apiKey;

  @Value("${spring.ai.naver.base-url}")
  private String baseUrl;

  @Value("${spring.ai.naver.chat.completions-path}")
  private String completionsPath;

  @Value("${spring.ai.naver.chat.options.model}")
  private String chatModel;

  @Value("${spring.ai.naver.embedding.embeddings-path}")
  private String embeddingsPath;

  @Value("${spring.ai.naver.embedding.options.model}")
  private String embeddingModel;

  @Value("${spring.ai.naver.embedding.options.dimensions}")
  private Integer embeddingDimensions;

  @Value("${spring.ai.naver.embedding.options.encoding-format}")
  private String embeddingEncodingFormat;

  @Bean("naverChatModel")
  public OpenAiChatModel naverChatModel() {
    OpenAiApi openAiApi =
        OpenAiApi.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .completionsPath(completionsPath)
            .embeddingsPath(embeddingsPath)
            .build();

    return OpenAiChatModel.builder()
        .openAiApi(openAiApi)
        .defaultOptions(OpenAiChatOptions.builder().model(chatModel).temperature(0.2d).build())
        .build();
  }

  @Bean("naverEmbeddingModel")
  public OpenAiEmbeddingModel naverEmbeddingModel(RestClient.Builder embeddingRestClientBuilder) {
    OpenAiApi openAiApi =
        OpenAiApi.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .completionsPath(completionsPath)
            .restClientBuilder(embeddingRestClientBuilder)
            .embeddingsPath(embeddingsPath)
            .build();

    return new OpenAiEmbeddingModel(
        openAiApi,
        MetadataMode.EMBED,
        OpenAiEmbeddingOptions.builder()
            .model(embeddingModel)
            .dimensions(embeddingDimensions)
            .encodingFormat(embeddingEncodingFormat)
            .build(),
        RetryUtils.DEFAULT_RETRY_TEMPLATE);
  }
}
