package com.swygbro.airoad.backend.ai.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class OpenAiChatModelConfig {

  @Value("${spring.ai.openai.api-key}")
  private String apiKey;

  @Value("${spring.ai.openai.base-url}")
  private String baseUrl;

  @Value("${spring.ai.openai.chat.options.model}")
  private String model;

  @Value("${spring.ai.openai.chat.completions-path}")
  private String completionsPath;

  @Value("${spring.ai.openai.embedding.embeddings-path}")
  private String embeddingsPath;

  @Bean("openAiChatModel")
  public OpenAiChatModel openAiChatModel() {
    OpenAiApi openAiApi =
        OpenAiApi.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .completionsPath(completionsPath)
            .embeddingsPath(embeddingsPath)
            .build();

    return OpenAiChatModel.builder()
        .openAiApi(openAiApi)
        .defaultOptions(OpenAiChatOptions.builder().model(model).build())
        .build();
  }
}
