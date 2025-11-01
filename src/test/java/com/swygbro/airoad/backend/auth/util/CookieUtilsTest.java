package com.swygbro.airoad.backend.auth.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
class CookieUtilsTest {

  private CookieUtils cookieUtils;

  @BeforeEach
  void setUp() {
    cookieUtils = new CookieUtils();
  }

  @Nested
  @DisplayName("getCookie 메서드는")
  class GetCookie {

    @Test
    @DisplayName("요청에서 이름으로 쿠키를 찾아 반환한다")
    void shouldReturnCookieByName() {
      // given
      HttpServletRequest request = mock(HttpServletRequest.class);
      Cookie cookie1 = new Cookie("name1", "value1");
      Cookie cookie2 = new Cookie("name2", "value2");
      when(request.getCookies()).thenReturn(new Cookie[] {cookie1, cookie2});

      // when
      Optional<Cookie> result = cookieUtils.getCookie(request, "name2");

      // then
      assertThat(result).isPresent();
      assertThat(result.get().getName()).isEqualTo("name2");
      assertThat(result.get().getValue()).isEqualTo("value2");
    }

    @Test
    @DisplayName("쿠키가 없으면 빈 Optional을 반환한다")
    void shouldReturnEmptyWhenCookieNotFound() {
      // given
      HttpServletRequest request = mock(HttpServletRequest.class);
      Cookie cookie1 = new Cookie("name1", "value1");
      when(request.getCookies()).thenReturn(new Cookie[] {cookie1});

      // when
      Optional<Cookie> result = cookieUtils.getCookie(request, "nonexistent");

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("요청에 쿠키가 전혀 없으면 빈 Optional을 반환한다")
    void shouldReturnEmptyWhenNoCookies() {
      // given
      HttpServletRequest request = mock(HttpServletRequest.class);
      when(request.getCookies()).thenReturn(null);

      // when
      Optional<Cookie> result = cookieUtils.getCookie(request, "anyName");

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("요청이 null이면 빈 Optional을 반환한다")
    void shouldReturnEmptyWhenRequestIsNull() {
      // when
      Optional<Cookie> result = cookieUtils.getCookie(null, "anyName");

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("쿠키 이름이 빈 문자열이면 빈 Optional을 반환한다")
    void shouldReturnEmptyWhenNameIsEmpty() {
      // given
      HttpServletRequest request = mock(HttpServletRequest.class);

      // when
      Optional<Cookie> result = cookieUtils.getCookie(request, "");

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("addCookie 메서드는")
  class AddCookie {

    @Test
    @DisplayName("쿠키를 응답에 추가한다")
    void shouldAddCookieToResponse() {
      // given
      HttpServletResponse response = mock(HttpServletResponse.class);
      ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

      // when
      cookieUtils.addCookie(response, "testName", "testValue", 3600);

      // then
      Mockito.verify(response).addCookie(cookieCaptor.capture());
      Cookie addedCookie = cookieCaptor.getValue();
      assertThat(addedCookie.getName()).isEqualTo("testName");
      assertThat(addedCookie.getValue()).isEqualTo("testValue");
      assertThat(addedCookie.getMaxAge()).isEqualTo(3600);
      assertThat(addedCookie.getPath()).isEqualTo("/");
      assertThat(addedCookie.isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("maxAge가 0이면 쿠키가 즉시 만료된다")
    void shouldSetMaxAgeToZero() {
      // given
      HttpServletResponse response = mock(HttpServletResponse.class);
      ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

      // when
      cookieUtils.addCookie(response, "testName", "testValue", 0);

      // then
      Mockito.verify(response).addCookie(cookieCaptor.capture());
      Cookie addedCookie = cookieCaptor.getValue();
      assertThat(addedCookie.getMaxAge()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("deleteCookie 메서드는")
  class DeleteCookie {

    @Test
    @DisplayName("요청에서 쿠키를 찾아 삭제한다")
    void shouldDeleteCookie() {
      // given
      HttpServletRequest request = mock(HttpServletRequest.class);
      HttpServletResponse response = mock(HttpServletResponse.class);
      Cookie cookie = new Cookie("testName", "testValue");
      cookie.setPath("/api");
      when(request.getCookies()).thenReturn(new Cookie[] {cookie});
      ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

      // when
      cookieUtils.deleteCookie(request, response, "testName");

      // then
      Mockito.verify(response).addCookie(cookieCaptor.capture());
      Cookie deletedCookie = cookieCaptor.getValue();
      assertThat(deletedCookie.getValue()).isEmpty();
      assertThat(deletedCookie.getMaxAge()).isEqualTo(0);
      assertThat(deletedCookie.getPath()).isEqualTo("/api");
    }

    @Test
    @DisplayName("쿠키에 경로가 없으면 기본 경로로 삭제한다")
    void shouldDeleteCookieWithDefaultPath() {
      // given
      HttpServletRequest request = mock(HttpServletRequest.class);
      HttpServletResponse response = mock(HttpServletResponse.class);
      Cookie cookie = new Cookie("testName", "testValue");
      when(request.getCookies()).thenReturn(new Cookie[] {cookie});
      ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

      // when
      cookieUtils.deleteCookie(request, response, "testName");

      // then
      Mockito.verify(response).addCookie(cookieCaptor.capture());
      Cookie deletedCookie = cookieCaptor.getValue();
      assertThat(deletedCookie.getPath()).isEqualTo("/");
    }

    @Test
    @DisplayName("요청이 null이면 아무것도 하지 않는다")
    void shouldDoNothingWhenRequestIsNull() {
      // given
      HttpServletResponse response = mock(HttpServletResponse.class);

      // when
      cookieUtils.deleteCookie(null, response, "testName");

      // then
      Mockito.verifyNoInteractions(response);
    }

    @Test
    @DisplayName("응답이 null이면 아무것도 하지 않는다")
    void shouldDoNothingWhenResponseIsNull() {
      // given
      HttpServletRequest request = mock(HttpServletRequest.class);

      // when
      cookieUtils.deleteCookie(request, null, "testName");

      // then
      Mockito.verifyNoInteractions(request);
    }

    @Test
    @DisplayName("쿠키 이름이 빈 문자열이면 아무것도 하지 않는다")
    void shouldDoNothingWhenNameIsEmpty() {
      // given
      HttpServletRequest request = mock(HttpServletRequest.class);
      HttpServletResponse response = mock(HttpServletResponse.class);

      // when
      cookieUtils.deleteCookie(request, response, "");

      // then
      Mockito.verifyNoInteractions(request, response);
    }
  }

  @Nested
  @DisplayName("serialize 메서드는")
  class Serialize {

    @Test
    @DisplayName("객체를 Base64 인코딩된 JSON 문자열로 직렬화한다")
    void shouldSerializeObject() {
      // given
      TestObject testObject = new TestObject("testName", 123);

      // when
      String serialized = cookieUtils.serialize(testObject);

      // then
      assertThat(serialized).isNotNull();
      assertThat(serialized).isNotEmpty();

      // Verify it's valid Base64
      byte[] decoded = Base64.getUrlDecoder().decode(serialized);
      String json = new String(decoded, StandardCharsets.UTF_8);
      assertThat(json).contains("testName");
      assertThat(json).contains("123");
    }

    @Test
    @DisplayName("null 객체를 직렬화해도 정상 처리된다")
    void shouldSerializeNull() {
      // when
      String serialized = cookieUtils.serialize(null);

      // then
      assertThat(serialized).isNotNull();
    }
  }

  @Nested
  @DisplayName("deserialize 메서드는")
  class Deserialize {

    @Test
    @DisplayName("쿠키가 null이면 예외가 발생한다")
    void shouldThrowExceptionWhenCookieIsNull() {
      // when & then
      assertThatThrownBy(() -> cookieUtils.deserialize(null, TestObject.class))
          .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠키 값이 빈 문자열이면 예외가 발생한다")
    void shouldThrowExceptionWhenCookieValueIsEmpty() {
      // given
      Cookie cookie = new Cookie("testCookie", "");

      // when & then
      assertThatThrownBy(() -> cookieUtils.deserialize(cookie, TestObject.class))
          .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("클래스 타입이 null이면 예외가 발생한다")
    void shouldThrowExceptionWhenClassIsNull() {
      // given
      Cookie cookie = new Cookie("testCookie", "someValue");

      // when & then
      assertThatThrownBy(() -> cookieUtils.deserialize(cookie, null))
          .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("유효하지 않은 Base64 문자열이면 예외가 발생한다")
    void shouldThrowExceptionWhenInvalidBase64() {
      // given
      Cookie cookie = new Cookie("testCookie", "invalid-base64!!!");

      // when & then
      assertThatThrownBy(() -> cookieUtils.deserialize(cookie, TestObject.class))
          .isInstanceOf(BusinessException.class);
    }
  }

  // Test helper class
  static class TestObject {
    private String name;
    private int value;

    public TestObject() {}

    public TestObject(String name, int value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getValue() {
      return value;
    }

    public void setValue(int value) {
      this.value = value;
    }
  }
}
