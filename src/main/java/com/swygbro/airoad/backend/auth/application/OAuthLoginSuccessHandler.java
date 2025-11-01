package com.swygbro.airoad.backend.auth.application;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.auth.domain.entity.TokenType;
import com.swygbro.airoad.backend.auth.domain.info.UserPrincipal;
import com.swygbro.airoad.backend.auth.filter.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  @Value("${app.oauth2.access}")
  private String ACCESS_TOKEN_REDIRECT_URI;

  @Value("${app.oauth2.redirect-uri}")
  private String redirectUri;

  private final JwtTokenProvider jwtTokenProvider;
  private final TokenService tokenService;

  @Override
  @Transactional
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {

    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

    redirectWithTokens(request, response, userPrincipal);
  }

  private void redirectWithTokens(
      HttpServletRequest request, HttpServletResponse response, UserPrincipal userPrincipal)
      throws IOException {

    String email = userPrincipal.getUsername();

    String accessToken = jwtTokenProvider.generateAccessToken(email);

    String refreshToken = jwtTokenProvider.generateRefreshToken(email);

    tokenService.createRefreshToken(refreshToken, email);

    String redirectUrl =
        String.format(
            ACCESS_TOKEN_REDIRECT_URI,
            this.redirectUri,
            accessToken,
            refreshToken,
            TokenType.BEARER.getValue());

    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
  }
}
