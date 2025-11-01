package com.swygbro.airoad.backend.auth.infrastructure;

import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.swygbro.airoad.backend.auth.util.CookieUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class HttpCookieOAuth2AuthorizationRequestRepositoryTest {

  @Mock private CookieUtils cookieUtils;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @InjectMocks private HttpCookieOAuth2AuthorizationRequestRepository repository;

  private OAuth2AuthorizationRequest authorizationRequest;

  @BeforeEach
  void setUp() {
    // Set field values using ReflectionTestUtils
    ReflectionTestUtils.setField(repository, "authRequestName", "oauth2_auth_request");
    ReflectionTestUtils.setField(repository, "redirectUriName", "redirect_uri");
    ReflectionTestUtils.setField(repository, "modeName", "mode");
    ReflectionTestUtils.setField(repository, "expireSeconds", 180);

    authorizationRequest =
        OAuth2AuthorizationRequest.authorizationCode()
            .clientId("test-client-id")
            .authorizationUri("https://provider.com/oauth/authorize")
            .redirectUri("http://localhost:8080/oauth2/callback/google")
            .build();
  }

  @Nested
  @DisplayName("loadAuthorizationRequest 메서드는")
  class LoadAuthorizationRequest {

    @Test
    @DisplayName("쿠키에서 OAuth2AuthorizationRequest를 로드한다")
    void shouldLoadAuthorizationRequest() {
      // given
      Cookie cookie = new Cookie("oauth2_auth_request", "serialized-value");
      given(cookieUtils.getCookie(request, "oauth2_auth_request")).willReturn(Optional.of(cookie));
      given(cookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class))
          .willReturn(authorizationRequest);

      // when
      OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getClientId()).isEqualTo("test-client-id");
      verify(cookieUtils).getCookie(request, "oauth2_auth_request");
      verify(cookieUtils).deserialize(cookie, OAuth2AuthorizationRequest.class);
    }

    @Test
    @DisplayName("쿠키가 없으면 null을 반환한다")
    void shouldReturnNullWhenCookieNotFound() {
      // given
      given(cookieUtils.getCookie(request, "oauth2_auth_request")).willReturn(Optional.empty());

      // when
      OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

      // then
      assertThat(result).isNull();
      verify(cookieUtils).getCookie(request, "oauth2_auth_request");
      verify(cookieUtils, never()).deserialize(any(), any());
    }
  }

  @Nested
  @DisplayName("saveAuthorizationRequest 메서드는")
  class SaveAuthorizationRequest {

    @Test
    @DisplayName("authorizationRequest가 null이 아니면 쿠키에 저장한다")
    void shouldSaveAuthorizationRequest() {
      // given
      String serializedRequest = "serialized-auth-request";
      given(cookieUtils.serialize(authorizationRequest)).willReturn(serializedRequest);

      // when
      repository.saveAuthorizationRequest(authorizationRequest, request, response);

      // then
      verify(cookieUtils).serialize(authorizationRequest);
      verify(cookieUtils).addCookie(response, "oauth2_auth_request", serializedRequest, 180);
    }

    @Test
    @DisplayName("redirect_uri 파라미터가 있으면 쿠키에 저장한다")
    void shouldSaveRedirectUriWhenPresent() {
      // given
      String redirectUri = "http://localhost:3000/oauth/callback";
      String serializedRequest = "serialized-auth-request";
      given(cookieUtils.serialize(authorizationRequest)).willReturn(serializedRequest);
      given(request.getParameter("redirect_uri")).willReturn(redirectUri);
      given(request.getParameter("mode")).willReturn(null);

      // when
      repository.saveAuthorizationRequest(authorizationRequest, request, response);

      // then
      verify(cookieUtils).addCookie(response, "oauth2_auth_request", serializedRequest, 180);
      verify(cookieUtils).addCookie(response, "redirect_uri", redirectUri, 180);
    }

    @Test
    @DisplayName("mode 파라미터가 있으면 쿠키에 저장한다")
    void shouldSaveModeWhenPresent() {
      // given
      String mode = "login";
      String serializedRequest = "serialized-auth-request";
      given(cookieUtils.serialize(authorizationRequest)).willReturn(serializedRequest);
      given(request.getParameter("redirect_uri")).willReturn(null);
      given(request.getParameter("mode")).willReturn(mode);

      // when
      repository.saveAuthorizationRequest(authorizationRequest, request, response);

      // then
      verify(cookieUtils).addCookie(response, "oauth2_auth_request", serializedRequest, 180);
      verify(cookieUtils).addCookie(response, "mode", mode, 180);
    }

    @Test
    @DisplayName("redirect_uri와 mode가 모두 있으면 둘 다 저장한다")
    void shouldSaveBothRedirectUriAndMode() {
      // given
      String redirectUri = "http://localhost:3000/oauth/callback";
      String mode = "login";
      String serializedRequest = "serialized-auth-request";
      given(cookieUtils.serialize(authorizationRequest)).willReturn(serializedRequest);
      given(request.getParameter("redirect_uri")).willReturn(redirectUri);
      given(request.getParameter("mode")).willReturn(mode);

      // when
      repository.saveAuthorizationRequest(authorizationRequest, request, response);

      // then
      verify(cookieUtils).addCookie(response, "oauth2_auth_request", serializedRequest, 180);
      verify(cookieUtils).addCookie(response, "redirect_uri", redirectUri, 180);
      verify(cookieUtils).addCookie(response, "mode", mode, 180);
    }

    @Test
    @DisplayName("authorizationRequest가 null이면 모든 쿠키를 삭제한다")
    void shouldDeleteCookiesWhenAuthorizationRequestIsNull() {
      // when
      repository.saveAuthorizationRequest(null, request, response);

      // then
      verify(cookieUtils).deleteCookie(request, response, "oauth2_auth_request");
      verify(cookieUtils).deleteCookie(request, response, "redirect_uri");
      verify(cookieUtils).deleteCookie(request, response, "mode");
      verify(cookieUtils, never()).serialize(any());
      verify(cookieUtils, never()).addCookie(any(), any(), any(), any(int.class));
    }

    @Test
    @DisplayName("redirect_uri가 빈 문자열이면 저장하지 않는다")
    void shouldNotSaveRedirectUriWhenEmpty() {
      // given
      String serializedRequest = "serialized-auth-request";
      given(cookieUtils.serialize(authorizationRequest)).willReturn(serializedRequest);
      given(request.getParameter("redirect_uri")).willReturn("");

      // when
      repository.saveAuthorizationRequest(authorizationRequest, request, response);

      // then
      verify(cookieUtils).addCookie(response, "oauth2_auth_request", serializedRequest, 180);
      verify(cookieUtils, never())
          .addCookie(eq(response), eq("redirect_uri"), any(), any(int.class));
    }
  }

  @Nested
  @DisplayName("removeAuthorizationRequest 메서드는")
  class RemoveAuthorizationRequest {

    @Test
    @DisplayName("authorizationRequest를 로드하고 쿠키를 삭제한다")
    void shouldLoadAndRemoveAuthorizationRequest() {
      // given
      Cookie authCookie = new Cookie("oauth2_auth_request", "serialized-value");
      given(cookieUtils.getCookie(request, "oauth2_auth_request"))
          .willReturn(Optional.of(authCookie));
      given(cookieUtils.deserialize(authCookie, OAuth2AuthorizationRequest.class))
          .willReturn(authorizationRequest);
      given(cookieUtils.getCookie(request, "redirect_uri")).willReturn(Optional.empty());
      given(cookieUtils.getCookie(request, "mode")).willReturn(Optional.empty());

      // when
      OAuth2AuthorizationRequest result = repository.removeAuthorizationRequest(request, response);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getClientId()).isEqualTo("test-client-id");
      verify(cookieUtils).deleteCookie(request, response, "oauth2_auth_request");
      verify(cookieUtils).deleteCookie(request, response, "redirect_uri");
      verify(cookieUtils).deleteCookie(request, response, "mode");
    }

    @Test
    @DisplayName("redirect_uri 쿠키가 있으면 request attribute로 보존한다")
    void shouldPreserveRedirectUriInRequestAttribute() {
      // given
      Cookie authCookie = new Cookie("oauth2_auth_request", "serialized-value");
      Cookie redirectCookie = new Cookie("redirect_uri", "http://localhost:3000/callback");
      given(cookieUtils.getCookie(request, "oauth2_auth_request"))
          .willReturn(Optional.of(authCookie));
      given(cookieUtils.deserialize(authCookie, OAuth2AuthorizationRequest.class))
          .willReturn(authorizationRequest);
      given(cookieUtils.getCookie(request, "redirect_uri")).willReturn(Optional.of(redirectCookie));
      given(cookieUtils.getCookie(request, "mode")).willReturn(Optional.empty());

      // when
      repository.removeAuthorizationRequest(request, response);

      // then
      verify(request)
          .setAttribute(
              HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM,
              "http://localhost:3000/callback");
    }

    @Test
    @DisplayName("mode 쿠키가 있으면 request attribute로 보존한다")
    void shouldPreserveModeInRequestAttribute() {
      // given
      Cookie authCookie = new Cookie("oauth2_auth_request", "serialized-value");
      Cookie modeCookie = new Cookie("mode", "login");
      given(cookieUtils.getCookie(request, "oauth2_auth_request"))
          .willReturn(Optional.of(authCookie));
      given(cookieUtils.deserialize(authCookie, OAuth2AuthorizationRequest.class))
          .willReturn(authorizationRequest);
      given(cookieUtils.getCookie(request, "redirect_uri")).willReturn(Optional.empty());
      given(cookieUtils.getCookie(request, "mode")).willReturn(Optional.of(modeCookie));

      // when
      repository.removeAuthorizationRequest(request, response);

      // then
      verify(request)
          .setAttribute(HttpCookieOAuth2AuthorizationRequestRepository.MODE_PARAM, "login");
    }

    @Test
    @DisplayName("redirect_uri와 mode가 모두 있으면 둘 다 request attribute로 보존한다")
    void shouldPreserveBothRedirectUriAndMode() {
      // given
      Cookie authCookie = new Cookie("oauth2_auth_request", "serialized-value");
      Cookie redirectCookie = new Cookie("redirect_uri", "http://localhost:3000/callback");
      Cookie modeCookie = new Cookie("mode", "login");
      given(cookieUtils.getCookie(request, "oauth2_auth_request"))
          .willReturn(Optional.of(authCookie));
      given(cookieUtils.deserialize(authCookie, OAuth2AuthorizationRequest.class))
          .willReturn(authorizationRequest);
      given(cookieUtils.getCookie(request, "redirect_uri")).willReturn(Optional.of(redirectCookie));
      given(cookieUtils.getCookie(request, "mode")).willReturn(Optional.of(modeCookie));

      // when
      repository.removeAuthorizationRequest(request, response);

      // then
      verify(request)
          .setAttribute(
              HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM,
              "http://localhost:3000/callback");
      verify(request)
          .setAttribute(HttpCookieOAuth2AuthorizationRequestRepository.MODE_PARAM, "login");
    }

    @Test
    @DisplayName("authorizationRequest가 null이면 쿠키를 삭제하지 않는다")
    void shouldNotDeleteCookiesWhenAuthorizationRequestIsNull() {
      // given
      given(cookieUtils.getCookie(request, "oauth2_auth_request")).willReturn(Optional.empty());
      given(cookieUtils.getCookie(request, "redirect_uri")).willReturn(Optional.empty());
      given(cookieUtils.getCookie(request, "mode")).willReturn(Optional.empty());

      // when
      OAuth2AuthorizationRequest result = repository.removeAuthorizationRequest(request, response);

      // then
      assertThat(result).isNull();
      verify(cookieUtils, never()).deleteCookie(any(), any(), eq("oauth2_auth_request"));
      verify(cookieUtils, never()).deleteCookie(any(), any(), eq("redirect_uri"));
      verify(cookieUtils, never()).deleteCookie(any(), any(), eq("mode"));
    }
  }
}
