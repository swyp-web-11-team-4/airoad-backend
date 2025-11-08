package com.swygbro.airoad.backend.auth.infrastructure;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 인증 후 리다이렉트 URL을 결정하는 유틸리티 클래스
 *
 * <p>프론트엔드 출처에 따라 localhost 또는 배포 URL로 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2RedirectUrlResolver {

  private static final String LOCAL_CLIENT_BASE_URL = "http://localhost:5173";

  private final CustomOAuth2AuthorizationRequestRepository authorizationRequestRepository;

  @Value("${spring.security.oauth2.client.redirect.base-url}")
  private String clientBaseUrl;

  /**
   * 프론트엔드 출처에 따라 적절한 리다이렉트 Base URL을 결정
   *
   * @param request HTTP 요청
   * @param handlerName 핸들러 이름 (로깅용)
   * @return localhost 또는 배포 URL
   */
  public String resolveBaseUrl(HttpServletRequest request, String handlerName) {
    String frontendOrigin = authorizationRequestRepository.getFrontendOrigin(request);

    log.info("=== OAuth2 Authentication {} - Redirect Base URL Detection ===", handlerName);
    log.info("[{}] Saved frontend origin: {}", handlerName, frontendOrigin);

    if (isLocalhost(frontendOrigin)) {
      log.info(
          "[{}] Detected localhost origin - using localClientBaseUrl: {}",
          handlerName,
          LOCAL_CLIENT_BASE_URL);
      return LOCAL_CLIENT_BASE_URL;
    }

    log.info("[{}] Using default clientBaseUrl: {}", handlerName, clientBaseUrl);
    return clientBaseUrl;
  }

  /**
   * URL의 호스트가 localhost인지 정확하게 판단
   *
   * @param url 검사할 URL 또는 origin
   * @return localhost 또는 127.0.0.1이면 true
   */
  private boolean isLocalhost(String url) {
    if (url == null || url.isEmpty()) {
      return false;
    }

    try {
      URI uri = new URI(url);
      String host = uri.getHost();

      if (host == null) {
        return false;
      }

      return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
    } catch (Exception e) {
      log.warn("Failed to parse URL: {}", url, e);
      return false;
    }
  }
}
