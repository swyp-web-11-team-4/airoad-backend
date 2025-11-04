package com.swygbro.airoad.backend.auth.presentation.web;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

/** OAuth2 로그인 실패 시 처리하는 핸들러 */
@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

  @Value("${spring.security.oauth2.client.redirect.base-url}")
  private String clientBaseUrl;

  @Value("${spring.security.oauth2.client.redirect.callback-path}")
  private String clientCallbackPath;

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {

    log.error("OAuth2 authentication failed: {}", exception.getMessage());

    String url =
        UriComponentsBuilder.fromUriString(clientBaseUrl + clientCallbackPath)
            .queryParam("status", "error")
            .toUriString();

    getRedirectStrategy().sendRedirect(request, response, url);
  }
}
