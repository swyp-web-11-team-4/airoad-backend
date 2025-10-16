package com.swygbro.airoad.backend.auth.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.swygbro.airoad.backend.auth.domain.entity.TokenType;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String token = getBearerToken(request);

    // 토큰이 있는 경우에만 검증
    if (StringUtils.hasText(token)) {
      try {
        if (jwtTokenProvider.validateAccessToken(token)) {
          Authentication authentication =
              jwtTokenProvider.getAuthentication(token, TokenType.ACCESS_TOKEN);
          SecurityContextHolder.getContext().setAuthentication(authentication);
          log.debug("Security Context에 '{}' 인증 정보를 저장했습니다.", authentication.getName());
        }
      } catch (ExpiredJwtException e) {
        log.error("토큰 인증 실패: {}", e.getMessage());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  private String getBearerToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");

    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }

    return null;
  }
}
