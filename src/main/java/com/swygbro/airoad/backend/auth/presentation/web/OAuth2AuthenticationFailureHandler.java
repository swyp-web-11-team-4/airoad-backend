package com.swygbro.airoad.backend.auth.presentation.web;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.swygbro.airoad.backend.auth.infrastructure.CustomOAuth2AuthorizationRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** OAuth2 로그인 실패 시 처리하는 핸들러 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

  private final CustomOAuth2AuthorizationRequestRepository authorizationRequestRepository;

  @Value("${spring.security.oauth2.client.redirect.base-url}")
  private String clientBaseUrl;

  @Value("${spring.security.oauth2.client.redirect.local-base-url}")
  private String localClientBaseUrl;

  @Value("${spring.security.oauth2.client.redirect.callback-path}")
  private String clientCallbackPath;

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {

    log.error("OAuth2 authentication failed: {}", exception.getMessage());

    try {
      String baseUrl = getBaseUrl(request);
      String url =
          UriComponentsBuilder.fromUriString(baseUrl + clientCallbackPath)
              .queryParam("status", "error")
              .toUriString();

      getRedirectStrategy().sendRedirect(request, response, url);
    } finally {
      // 세션에서 프론트엔드 출처 정보 제거
      authorizationRequestRepository.removeFrontendOrigin(request);
    }
  }

  private String getBaseUrl(HttpServletRequest request) {
    String frontendOrigin = authorizationRequestRepository.getFrontendOrigin(request);

    log.info("=== OAuth2 Authentication Failure - Redirect Base URL Detection ===");
    log.info("[Failure Handler] Saved frontend origin: {}", frontendOrigin);

    // 프론트엔드 출처가 localhost인 경우
    if (frontendOrigin != null
        && (frontendOrigin.contains("localhost") || frontendOrigin.contains("127.0.0.1"))) {
      log.info(
          "[Failure Handler] Detected localhost origin - using localClientBaseUrl: {}",
          localClientBaseUrl);
      return localClientBaseUrl;
    }

    // 그 외의 경우 배포된 사이트로 리다이렉트
    log.info("[Failure Handler] Using default clientBaseUrl: {}", clientBaseUrl);
    return clientBaseUrl;
  }
}
