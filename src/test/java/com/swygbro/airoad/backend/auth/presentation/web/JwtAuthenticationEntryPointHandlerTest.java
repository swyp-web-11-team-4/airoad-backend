package com.swygbro.airoad.backend.auth.presentation.web;

import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swygbro.airoad.backend.auth.exception.AuthErrorCode;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.common.domain.dto.ErrorResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationEntryPoint 클래스는")
class JwtAuthenticationEntryPointHandlerTest {

  @Mock private ObjectMapper objectMapper;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @InjectMocks private JwtAuthenticationEntryPointHandler jwtAuthenticationEntryPointHandler;

  @Nested
  @DisplayName("commence 메서드는")
  class Commence {

    @Test
    @DisplayName("인증되지 않은 요청을 받으면 401 상태 코드를 설정한다")
    void setUnauthorizedStatus() throws Exception {
      // given
      AuthenticationException authException = new InsufficientAuthenticationException("인증 필요");
      given(request.getRequestURI()).willReturn("/api/v1/protected");

      StringWriter stringWriter = new StringWriter();
      PrintWriter writer = new PrintWriter(stringWriter);
      given(response.getWriter()).willReturn(writer);
      given(objectMapper.writeValueAsString(any())).willReturn("{}");

      // when
      jwtAuthenticationEntryPointHandler.commence(request, response, authException);

      // then
      verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("인증되지 않은 요청을 받으면 JSON 응답 Content-Type을 설정한다")
    void setJsonContentType() throws Exception {
      // given
      AuthenticationException authException = new InsufficientAuthenticationException("인증 필요");
      given(request.getRequestURI()).willReturn("/api/v1/protected");

      StringWriter stringWriter = new StringWriter();
      PrintWriter writer = new PrintWriter(stringWriter);
      given(response.getWriter()).willReturn(writer);
      given(objectMapper.writeValueAsString(any())).willReturn("{}");

      // when
      jwtAuthenticationEntryPointHandler.commence(request, response, authException);

      // then
      verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    @DisplayName("인증되지 않은 요청을 받으면 UTF-8 인코딩을 설정한다")
    void setUtf8Encoding() throws Exception {
      // given
      AuthenticationException authException = new InsufficientAuthenticationException("인증 필요");
      given(request.getRequestURI()).willReturn("/api/v1/protected");

      StringWriter stringWriter = new StringWriter();
      PrintWriter writer = new PrintWriter(stringWriter);
      given(response.getWriter()).willReturn(writer);
      given(objectMapper.writeValueAsString(any())).willReturn("{}");

      // when
      jwtAuthenticationEntryPointHandler.commence(request, response, authException);

      // then
      verify(response).setCharacterEncoding("UTF-8");
    }

    @Test
    @DisplayName("인증되지 않은 요청을 받으면 AUTHENTICATION_REQUIRED 에러 코드로 ErrorResponse를 생성한다")
    void createErrorResponseWithAuthenticationRequiredCode() throws Exception {
      // given
      AuthenticationException authException = new InsufficientAuthenticationException("인증 필요");
      String requestUri = "/api/v1/protected";
      given(request.getRequestURI()).willReturn(requestUri);

      StringWriter stringWriter = new StringWriter();
      PrintWriter writer = new PrintWriter(stringWriter);
      given(response.getWriter()).willReturn(writer);

      ObjectMapper realObjectMapper = new ObjectMapper();
      realObjectMapper.findAndRegisterModules();
      JwtAuthenticationEntryPointHandler entryPoint =
          new JwtAuthenticationEntryPointHandler(realObjectMapper);

      // when
      entryPoint.commence(request, response, authException);

      // then
      String jsonResponse = stringWriter.toString();
      assertThat(jsonResponse).contains("AUTH011");
      assertThat(jsonResponse).contains(requestUri);
    }

    @Test
    @DisplayName("인증되지 않은 요청을 받으면 실패 응답 형식으로 CommonResponse를 생성한다")
    void createCommonResponseWithErrorFormat() throws Exception {
      // given
      AuthenticationException authException = new InsufficientAuthenticationException("인증 필요");
      String requestUri = "/api/v1/protected";
      given(request.getRequestURI()).willReturn(requestUri);

      StringWriter stringWriter = new StringWriter();
      PrintWriter writer = new PrintWriter(stringWriter);
      given(response.getWriter()).willReturn(writer);

      ObjectMapper realObjectMapper = new ObjectMapper();
      realObjectMapper.findAndRegisterModules();
      JwtAuthenticationEntryPointHandler entryPoint =
          new JwtAuthenticationEntryPointHandler(realObjectMapper);

      // when
      entryPoint.commence(request, response, authException);

      // then
      String jsonResponse = stringWriter.toString();
      assertThat(jsonResponse).contains("\"success\":false");
      assertThat(jsonResponse).contains("\"status\":401");
    }

    @Test
    @DisplayName("인증되지 않은 요청을 받으면 요청 URI를 포함한 에러 응답을 생성한다")
    void includeRequestUriInErrorResponse() throws Exception {
      // given
      AuthenticationException authException = new InsufficientAuthenticationException("인증 필요");
      String requestUri = "/api/v1/members/profile";
      given(request.getRequestURI()).willReturn(requestUri);

      StringWriter stringWriter = new StringWriter();
      PrintWriter writer = new PrintWriter(stringWriter);
      given(response.getWriter()).willReturn(writer);

      ObjectMapper realObjectMapper = new ObjectMapper();
      realObjectMapper.findAndRegisterModules();
      JwtAuthenticationEntryPointHandler entryPoint =
          new JwtAuthenticationEntryPointHandler(realObjectMapper);

      // when
      entryPoint.commence(request, response, authException);

      // then
      String jsonResponse = stringWriter.toString();
      assertThat(jsonResponse).contains(requestUri);
    }

    @Test
    @DisplayName("인증되지 않은 요청을 받으면 JSON 형식의 에러 응답을 작성한다")
    void writeJsonErrorResponse() throws Exception {
      // given
      AuthenticationException authException = new InsufficientAuthenticationException("인증 필요");
      given(request.getRequestURI()).willReturn("/api/v1/protected");

      StringWriter stringWriter = new StringWriter();
      PrintWriter writer = new PrintWriter(stringWriter);
      given(response.getWriter()).willReturn(writer);

      ObjectMapper realObjectMapper = new ObjectMapper();
      realObjectMapper.findAndRegisterModules();
      JwtAuthenticationEntryPointHandler entryPoint =
          new JwtAuthenticationEntryPointHandler(realObjectMapper);

      AuthErrorCode errorCode = AuthErrorCode.AUTHENTICATION_REQUIRED;
      ErrorResponse errorResponse =
          ErrorResponse.of(errorCode.getCode(), errorCode.getDefaultMessage(), "/api/v1/protected");
      CommonResponse<ErrorResponse> expectedResponse =
          CommonResponse.error(errorCode.getHttpStatus(), errorResponse);

      // when
      entryPoint.commence(request, response, authException);

      // then
      String actualJson = stringWriter.toString();
      assertThat(actualJson).isNotEmpty();
      assertThat(actualJson).contains("success");
      assertThat(actualJson).contains("status");
      assertThat(actualJson).contains("data");
    }
  }
}
