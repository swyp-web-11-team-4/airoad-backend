package com.swygbro.airoad.backend.auth.presentation.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swygbro.airoad.backend.auth.exception.AuthErrorCode;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.common.domain.dto.ErrorResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 인증되지 않은 사용자가 보호된 리소스에 접근할 때 호출되는 핸들러 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPointHandler implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {

    log.warn(
        "Unauthorized request to: {} - {}", request.getRequestURI(), authException.getMessage());

    AuthErrorCode errorCode = AuthErrorCode.AUTHENTICATION_REQUIRED;

    response.setStatus(errorCode.getHttpStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    ErrorResponse errorResponse =
        ErrorResponse.of(
            errorCode.getCode(), errorCode.getDefaultMessage(), request.getRequestURI());

    CommonResponse<ErrorResponse> commonResponse =
        CommonResponse.error(errorCode.getHttpStatus(), errorResponse);

    response.getWriter().write(objectMapper.writeValueAsString(commonResponse));
  }
}
