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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swygbro.airoad.backend.auth.application.UserDetailsServiceImpl;
import com.swygbro.airoad.backend.auth.domain.entity.TokenType;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.common.domain.dto.ErrorResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserDetailsServiceImpl userDetailsService;
  private final ObjectMapper objectMapper;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {
      String authorizationHeader = request.getHeader("Authorization");
      String token = getTokenFromHeader(authorizationHeader);

      if (StringUtils.hasText(token)) {
        jwtTokenProvider.validateAccessToken(token);
        String email =
            jwtTokenProvider.getClaimFromToken(
                token, "email", String.class, TokenType.ACCESS_TOKEN);

        setAuthentication(email);
      }

      filterChain.doFilter(request, response);

    } catch (BusinessException e) {
      writeErrorResponse(request, response, e);
    }
  }

  private String getTokenFromHeader(String authorizationHeader) {
    if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
      return authorizationHeader.substring(7);
    }
    return null;
  }

  private void setAuthentication(String email) {
    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
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
    response.getWriter().write(objectMapper.writeValueAsString(commonResponse));
  }
}
