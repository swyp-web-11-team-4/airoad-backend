package com.swygbro.airoad.backend.auth.presentation.web;

import java.io.IOException;
import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.swygbro.airoad.backend.auth.application.AuthUseCase;
import com.swygbro.airoad.backend.auth.domain.dto.UserPrincipal;
import com.swygbro.airoad.backend.auth.domain.dto.response.TokenResponse;
import com.swygbro.airoad.backend.auth.infrastructure.CustomOAuth2AuthorizationRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** OAuth2 로그인 성공 시 JWT 토큰을 생성하여 클라이언트에 전달하는 핸들러 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private static final String LOCAL_CLIENT_BASE_URL = "http://localhost:5173";

  private final AuthUseCase authUseCase;
  private final CustomOAuth2AuthorizationRequestRepository authorizationRequestRepository;

  @Value("${spring.security.oauth2.client.redirect.base-url}")
  private String clientBaseUrl;

  @Value("${spring.security.oauth2.client.redirect.callback-path}")
  private String clientCallbackPath;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {

    log.debug("OAuth2 authentication success handler called");

    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
    String email = userPrincipal.getUsername();

    log.debug("OAuth2User attributes: {}", userPrincipal.getAttributes());
    log.debug("Email from OAuth2User: {}", email);

    String baseUrl = getBaseUrl(request);

    if (email == null) {
      log.error("Email not found in OAuth2User attributes");
      getRedirectStrategy()
          .sendRedirect(request, response, baseUrl + clientCallbackPath + "?status=error");
      return;
    }

    try {
      // JWT 토큰 생성 및 Refresh Token 저장
      TokenResponse tokenResponse = authUseCase.createTokens(email);

      String url =
          UriComponentsBuilder.fromUriString(baseUrl + clientCallbackPath)
              .queryParam("accessToken", tokenResponse.accessToken())
              .queryParam("refreshToken", tokenResponse.refreshToken())
              .toUriString();

      getRedirectStrategy().sendRedirect(request, response, url);

      log.debug("Redirect completed successfully");

    } catch (Exception e) {
      log.error("Error during OAuth2 authentication success handling", e);

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

    log.info("=== OAuth2 Authentication Success - Redirect Base URL Detection ===");
    log.info("[Success Handler] Saved frontend origin: {}", frontendOrigin);

    // 프론트엔드 출처가 localhost인 경우
    if (isLocalhost(frontendOrigin)) {
      log.info(
          "[Success Handler] Detected localhost origin - using localClientBaseUrl: {}",
          LOCAL_CLIENT_BASE_URL);
      return LOCAL_CLIENT_BASE_URL;
    }

    // 그 외의 경우 배포된 사이트로 리다이렉트
    log.info("[Success Handler] Using default clientBaseUrl: {}", clientBaseUrl);
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
      log.warn("[Success Handler] Failed to parse URL: {}", url, e);
      return false;
    }
  }
}
