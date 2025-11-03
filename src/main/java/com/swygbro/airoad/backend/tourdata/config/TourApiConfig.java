package com.swygbro.airoad.backend.tourdata.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** TourAPI 관련 설정 */
@Configuration
public class TourApiConfig {

  /**
   * TourAPI 호출용 RestClient.Builder
   *
   * @return RestClient.Builder with timeout settings
   */
  @Bean
  public RestClient.Builder tourApiRestClientBuilder() {
    return RestClient.builder().requestFactory(clientHttpRequestFactory());
  }

  /**
   * HTTP 요청 팩토리 (타임아웃 설정)
   *
   * @return ClientHttpRequestFactory
   */
  private ClientHttpRequestFactory clientHttpRequestFactory() {
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
    factory.setReadTimeout(Duration.ofSeconds(30));
    return factory;
  }
}
