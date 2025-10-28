package com.swygbro.airoad.backend.auth.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.swygbro.airoad.backend.auth.util.CookieUtils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
  public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
  public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
  public static final String MODE_PARAM_COOKIE_NAME = "mode";
  private static final int COOKIE_EXPIRE_SECONDS = 180;

  private final CookieUtils cookieUtils;

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    return cookieUtils
        .getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
        .map(cookie -> cookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class))
        .orElse(null);
  }

  @Override
  public void saveAuthorizationRequest(
      OAuth2AuthorizationRequest authorizationRequest,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (authorizationRequest == null) {
      cookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
      cookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
      cookieUtils.deleteCookie(request, response, MODE_PARAM_COOKIE_NAME);
      return;
    }

    cookieUtils.addCookie(
        response,
        OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
        cookieUtils.serialize(authorizationRequest),
        COOKIE_EXPIRE_SECONDS);

    String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
    if (StringUtils.hasText(redirectUriAfterLogin)) {
      cookieUtils.addCookie(
          response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
    }

    String mode = request.getParameter(MODE_PARAM_COOKIE_NAME);
    if (StringUtils.hasText(mode)) {
      cookieUtils.addCookie(response, MODE_PARAM_COOKIE_NAME, mode, COOKIE_EXPIRE_SECONDS);
    }
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(
      HttpServletRequest request, HttpServletResponse response) {
    OAuth2AuthorizationRequest authorizationRequest = this.loadAuthorizationRequest(request);

    if (authorizationRequest != null) {
      cookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
      cookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
      cookieUtils.deleteCookie(request, response, MODE_PARAM_COOKIE_NAME);
    }

    return authorizationRequest;
  }
}
