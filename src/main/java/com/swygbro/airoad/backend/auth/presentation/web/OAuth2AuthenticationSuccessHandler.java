package com.swygbro.airoad.backend.auth.presentation.web;

import java.io.IOException;

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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** OAuth2 로그인 성공 시 JWT 토큰을 생성하여 클라이언트에 전달하는 핸들러 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final AuthUseCase authUseCase;

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

    if (email == null) {
      log.error("Email not found in OAuth2User attributes");
      getRedirectStrategy()
          .sendRedirect(request, response, clientBaseUrl + clientCallbackPath + "?status=error");
      return;
    }

    try {
      // JWT 토큰 생성 및 Refresh Token 저장
      TokenResponse tokenResponse = authUseCase.createTokens(email);

      String url =
          UriComponentsBuilder.fromUriString(clientBaseUrl + clientCallbackPath)
              .queryParam("accessToken", tokenResponse.accessToken())
              .queryParam("refreshToken", tokenResponse.refreshToken())
              .toUriString();

      getRedirectStrategy().sendRedirect(request, response, url);

      log.debug("Redirect completed successfully");

    } catch (Exception e) {
      log.error("Error during OAuth2 authentication success handling", e);

      String url =
          UriComponentsBuilder.fromUriString(clientBaseUrl + clientCallbackPath)
              .queryParam("status", "error")
              .toUriString();

      getRedirectStrategy().sendRedirect(request, response, url);
    }
  }
}
