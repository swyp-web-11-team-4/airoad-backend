package com.swygbro.airoad.backend.ai.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class UpstageChatModelConfig {

  @Value("${spring.ai.upstage.api-key}")
  private String apiKey;

  @Value("${spring.ai.upstage.base-url}")
  private String baseUrl;

  @Value("${spring.ai.upstage.chat.options.model}")
  private String model;

  @Bean
  @Qualifier("upstageChatModel")
  public OpenAiChatModel upstageChatModel() {
    OpenAiApi openAiApi = OpenAiApi.builder().apiKey(apiKey).baseUrl(baseUrl).build();

    return OpenAiChatModel.builder()
        .openAiApi(openAiApi)
        .defaultOptions(OpenAiChatOptions.builder().model(model).build())
        .build();
  }
}
