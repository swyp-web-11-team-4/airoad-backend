package com.swygbro.airoad.backend.auth.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.swygbro.airoad.backend.auth.application.UserDetailsServiceImpl;
import com.swygbro.airoad.backend.auth.domain.entity.TokenType;
import com.swygbro.airoad.backend.auth.exception.AuthErrorCode;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.common.domain.dto.ErrorResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserDetailsServiceImpl userDetailsService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      String authorizationHeader = request.getHeader("Authorization");
      String token = getTokenFromHeader(authorizationHeader);

      if (StringUtils.hasText(token)) {
        jwtTokenProvider.validateAccessToken(token);
        Long userId =
            jwtTokenProvider.getClaimFromToken(token, "userId", Long.class, TokenType.ACCESS_TOKEN);
        setAuthentication(userId);
      }

      filterChain.doFilter(request, response);

    } catch (BusinessException e) {
      log.error("JWT 인증 중 예외 발생: {}", e.getMessage());
      writeErrorResponse(request, response, e);
    }
  }

  private String getTokenFromHeader(String authorizationHeader) {
    if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
      return authorizationHeader.substring(7);
    }
    return null;
  }

  private void setAuthentication(Long userId) {
    UserDetails userDetails = userDetailsService.loadUserByUserId(userId);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private void writeErrorResponse(
      HttpServletRequest request, HttpServletResponse response, BusinessException e)
      throws IOException {
    int status = e.getErrorCode().getHttpStatus().value();
    String code = e.getErrorCode().getCode();
    String message = e.getMessage();
    ErrorResponse errorResponse = ErrorResponse.of(code, message, request.getRequestURI());
    CommonResponse<ErrorResponse> commonResponse =
        CommonResponse.error(e.getErrorCode().getHttpStatus(), errorResponse);

    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    response
        .getWriter()
        .write(
            "{"
                + "\"success\": "
                + commonResponse.success()
                + ","
                + "\"status\": "
                + commonResponse.status()
                + ","
                + "\"data\": {"
                + "\"code\": \""
                + code
                + "\","
                + "\"message\": \""
                + message
                + "\""
                + "}"
                + "}");
  }
}
