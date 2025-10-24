package com.swygbro.airoad.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.swygbro.airoad.backend.content.infrastructure.interceptor.ClovaStudioEmbeddingInterceptor;

@Configuration
public class RestClientConfig {
  @Bean
  public RestClient.Builder restClientBuilder(
      ClovaStudioEmbeddingInterceptor clovaStudioEmbeddingInterceptor) {
    return RestClient.builder().requestInterceptor(clovaStudioEmbeddingInterceptor);
  }
}
