package com.swygbro.airoad.backend.auth.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 인증 요청 정보를 세션에 저장하는 커스텀 리포지토리
 *
 * <p>프론트엔드 출처(Origin/Referer)를 저장하여 OAuth2 콜백 후 적절한 URL로 리다이렉트하기 위해 사용
 */
@Slf4j
@Component
public class CustomOAuth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  private static final String AUTHORIZATION_REQUEST_SESSION_KEY = "OAUTH2_AUTHORIZATION_REQUEST";
  private static final String FRONTEND_ORIGIN_SESSION_KEY = "FRONTEND_ORIGIN";

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    return getAuthorizationRequestFromSession(request);
  }

  @Override
  public void saveAuthorizationRequest(
      OAuth2AuthorizationRequest authorizationRequest,
      HttpServletRequest request,
      HttpServletResponse response) {

    if (authorizationRequest == null) {
      removeAuthorizationRequest(request, response);
      return;
    }

    HttpSession session = request.getSession();

    // OAuth2 인증 요청 정보 저장
    session.setAttribute(AUTHORIZATION_REQUEST_SESSION_KEY, authorizationRequest);

    // 프론트엔드 출처 저장 (Origin 또는 Referer)
    String frontendOrigin = determineFrontendOrigin(request);
    if (frontendOrigin != null) {
      session.setAttribute(FRONTEND_ORIGIN_SESSION_KEY, frontendOrigin);
      log.debug("Saved frontend origin to session: {}", frontendOrigin);
    }
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(
      HttpServletRequest request, HttpServletResponse response) {
    OAuth2AuthorizationRequest authorizationRequest = getAuthorizationRequestFromSession(request);

    if (authorizationRequest != null) {
      HttpSession session = request.getSession(false);
      if (session != null) {
        session.removeAttribute(AUTHORIZATION_REQUEST_SESSION_KEY);
        // Frontend origin은 SuccessHandler/FailureHandler에서 사용 후 제거
      }
    }

    return authorizationRequest;
  }

  /**
   * 세션에 저장된 프론트엔드 출처를 가져옴
   *
   * @param request HTTP 요청
   * @return 저장된 프론트엔드 출처 또는 null
   */
  public String getFrontendOrigin(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      return (String) session.getAttribute(FRONTEND_ORIGIN_SESSION_KEY);
    }
    return null;
  }

  /**
   * 세션에서 프론트엔드 출처 정보를 제거
   *
   * @param request HTTP 요청
   */
  public void removeFrontendOrigin(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.removeAttribute(FRONTEND_ORIGIN_SESSION_KEY);
      log.debug("Removed frontend origin from session");
    }
  }

  private OAuth2AuthorizationRequest getAuthorizationRequestFromSession(
      HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      return (OAuth2AuthorizationRequest) session.getAttribute(AUTHORIZATION_REQUEST_SESSION_KEY);
    }
    return null;
  }

  /**
   * 요청에서 프론트엔드 출처를 판단
   *
   * @param request HTTP 요청
   * @return 프론트엔드 출처 (Origin 또는 Referer)
   */
  private String determineFrontendOrigin(HttpServletRequest request) {
    String origin = request.getHeader("Origin");
    if (origin != null && !origin.isEmpty()) {
      log.debug("Frontend origin from Origin header: {}", origin);
      return origin;
    }

    String referer = request.getHeader("Referer");
    if (referer != null && !referer.isEmpty()) {
      log.debug("Frontend origin from Referer header: {}", referer);
      return referer;
    }

    log.debug("No frontend origin found in request headers");
    return null;
  }
}
