package com.swygbro.airoad.backend.auth.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.swygbro.airoad.backend.auth.exception.AuthErrorCode;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CookieUtils {

  private final ObjectMapper objectMapper;

  public CookieUtils() {
    this.objectMapper = createSecureObjectMapper();
  }

  private ObjectMapper createSecureObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();

    // Spring Security Jackson Mixins 등록
    // OAuth2AuthorizationRequest, OAuth2User 등의 직렬화/역직렬화를 지원
    SecurityJackson2Modules.getModules(this.getClass().getClassLoader())
        .forEach(mapper::registerModule);

    // Java 8 Time API 지원
    mapper.registerModule(new JavaTimeModule());

    // ISO 8601 날짜 형식 사용
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    return mapper;
  }

  public Optional<Cookie> getCookie(HttpServletRequest request, String name) {
    if (request == null || !StringUtils.hasText(name)) {
      return Optional.empty();
    }

    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals(name)) {
          return Optional.of(cookie);
        }
      }
    }
    return Optional.empty();
  }

  /**
   * 응답에 쿠키를 추가합니다.
   *
   * @param response HTTP 응답
   * @param name 쿠키 이름
   * @param value 쿠키 값
   * @param maxAge 쿠키 유효 시간(초)
   * @throws BusinessException 쿠키 크기가 제한을 초과하는 경우
   */
  public void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
    Cookie cookie = new Cookie(name, value);
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    cookie.setMaxAge(maxAge);
    cookie.setSecure(true); // HTTPS에서만 전송
    cookie.setAttribute("SameSite", "Lax"); // CSRF 공격 방어
    response.addCookie(cookie);
  }

  public void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
    if (request == null || response == null || !StringUtils.hasText(name)) {
      return;
    }

    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals(name)) {
          cookie.setValue("");
          cookie.setPath("/");
          cookie.setMaxAge(0);
          cookie.setHttpOnly(true);
          cookie.setSecure(true);
          cookie.setAttribute("SameSite", "Lax");
          response.addCookie(cookie);
        }
      }
    }
  }

  public String serialize(Object object) {
    try {
      String json = objectMapper.writeValueAsString(object);
      return Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    } catch (JsonProcessingException e) {
      throw new BusinessException(AuthErrorCode.OAUTH2_COOKIE_SERIALIZATION_FAILED);
    }
  }

  public <T> T deserialize(Cookie cookie, Class<T> cls) {
    if (cookie == null || !StringUtils.hasText(cookie.getValue()) || cls == null) {
      throw new BusinessException(AuthErrorCode.OAUTH2_COOKIE_DESERIALIZATION_FAILED);
    }

    try {
      byte[] decodedBytes = Base64.getUrlDecoder().decode(cookie.getValue());
      String json = new String(decodedBytes, StandardCharsets.UTF_8);
      return objectMapper.readValue(json, cls);
    } catch (Exception e) {
      log.error("Failed to deserialize cookie: {}, error: {}", cookie.getName(), e.getMessage());
      throw new BusinessException(AuthErrorCode.OAUTH2_COOKIE_DESERIALIZATION_FAILED);
    }
  }
}
