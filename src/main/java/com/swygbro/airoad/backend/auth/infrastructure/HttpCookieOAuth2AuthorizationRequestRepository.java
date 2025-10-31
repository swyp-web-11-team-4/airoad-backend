package com.swygbro.airoad.backend.auth.infrastructure;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.swygbro.airoad.backend.auth.util.CookieUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  // Request attribute keys for passing data
  public static final String REDIRECT_URI_PARAM = "REDIRECT_URI_PARAM";
  public static final String MODE_PARAM = "MODE_PARAM";

  @Value("${app.oauth2.cookie.auth-request-name}")
  private String authRequestName;

  @Value("${app.oauth2.cookie.redirect-uri-name}")
  private String redirectUriName;

  @Value("${app.oauth2.cookie.mode-name}")
  private String modeName;

  @Value("${app.oauth2.cookie.expire-seconds}")
  private int expireSeconds;

  private final CookieUtils cookieUtils;

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    return cookieUtils
        .getCookie(request, authRequestName)
        .map(cookie -> cookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class))
        .orElse(null);
  }

  @Override
  public void saveAuthorizationRequest(
      OAuth2AuthorizationRequest authorizationRequest,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (authorizationRequest == null) {
      cookieUtils.deleteCookie(request, response, authRequestName);
      cookieUtils.deleteCookie(request, response, redirectUriName);
      cookieUtils.deleteCookie(request, response, modeName);
      return;
    }

    cookieUtils.addCookie(
        response, authRequestName, cookieUtils.serialize(authorizationRequest), expireSeconds);

    String redirectUriAfterLogin = request.getParameter(redirectUriName);
    log.info("Saving redirect URI: {}", redirectUriAfterLogin);

    if (StringUtils.hasText(redirectUriAfterLogin)) {
      cookieUtils.addCookie(response, redirectUriName, redirectUriAfterLogin, expireSeconds);
    }

    String mode = request.getParameter(modeName);
    log.info("Saving mode: {}", mode);

    if (StringUtils.hasText(mode)) {
      cookieUtils.addCookie(response, modeName, mode, expireSeconds);
    }
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(
      HttpServletRequest request, HttpServletResponse response) {
    OAuth2AuthorizationRequest authorizationRequest = this.loadAuthorizationRequest(request);

    // IMPORTANT: Save cookie values to request attributes before deleting
    // This allows the success handler to access them
    cookieUtils
        .getCookie(request, redirectUriName)
        .map(Cookie::getValue)
        .ifPresent(
            value -> {
              log.info("Preserving redirect_uri in request attribute: {}", value);
              request.setAttribute(REDIRECT_URI_PARAM, value);
            });

    cookieUtils
        .getCookie(request, modeName)
        .map(Cookie::getValue)
        .ifPresent(
            value -> {
              log.info("Preserving mode in request attribute: {}", value);
              request.setAttribute(MODE_PARAM, value);
            });

    // Now delete the cookies
    if (authorizationRequest != null) {
      cookieUtils.deleteCookie(request, response, authRequestName);
      cookieUtils.deleteCookie(request, response, redirectUriName);
      cookieUtils.deleteCookie(request, response, modeName);
    }

    return authorizationRequest;
  }
}
