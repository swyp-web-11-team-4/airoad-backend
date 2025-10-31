package com.swygbro.airoad.backend.auth.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swygbro.airoad.backend.auth.application.UserDetailsServiceImpl;
import com.swygbro.airoad.backend.auth.domain.entity.TokenType;
import com.swygbro.airoad.backend.auth.domain.info.UserPrincipal;
import com.swygbro.airoad.backend.auth.exception.AuthErrorCode;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class JwtAuthenticationFilterTest {

  @Mock private JwtTokenProvider jwtTokenProvider;

  @Mock private UserDetailsServiceImpl userDetailsService;

  @Mock private ObjectMapper objectMapper;

  @Mock private FilterChain filterChain;

  @InjectMocks private JwtAuthenticationFilter jwtAuthenticationFilter;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private Member testMember;
  private UserDetails userDetails;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    SecurityContextHolder.clearContext();

    testMember =
        Member.builder()
            .email("test@example.com")
            .name("Test User")
            .imageUrl("https://example.com/image.jpg")
            .provider(ProviderType.GOOGLE)
            .role(MemberRole.MEMBER)
            .build();

    userDetails = new UserPrincipal(testMember);
  }

  @Nested
  @DisplayName("doFilterInternal 메서드는")
  class DoFilterInternal {

    @Test
    @DisplayName("유효한 토큰이 있을 때 인증을 설정하고 필터 체인을 계속 진행한다")
    void shouldSetAuthenticationWhenTokenIsValid() throws ServletException, IOException {
      // given
      String token = "valid.jwt.token";
      String email = "test@example.com";
      request.addHeader("Authorization", "Bearer " + token);

      given(
              jwtTokenProvider.getClaimFromToken(
                  eq(token), eq("email"), eq(String.class), eq(TokenType.ACCESS_TOKEN)))
          .willReturn(email);
      given(userDetailsService.loadUserByUsername(email)).willReturn(userDetails);

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      verify(jwtTokenProvider).validateAccessToken(token);
      verify(jwtTokenProvider)
          .getClaimFromToken(eq(token), eq("email"), eq(String.class), eq(TokenType.ACCESS_TOKEN));
      verify(userDetailsService).loadUserByUsername(email);
      verify(filterChain).doFilter(request, response);

      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
      assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
          .isEqualTo(userDetails);
    }

    @Test
    @DisplayName("Authorization 헤더가 없을 때 인증 없이 필터 체인을 계속 진행한다")
    void shouldContinueFilterChainWhenNoAuthorizationHeader() throws ServletException, IOException {
      // given
      // Authorization 헤더 없음

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      verify(jwtTokenProvider, never()).validateAccessToken(anyString());
      verify(userDetailsService, never()).loadUserByUsername(anyString());
      verify(filterChain).doFilter(request, response);

      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Bearer로 시작하지 않는 토큰일 때 인증 없이 필터 체인을 계속 진행한다")
    void shouldContinueFilterChainWhenTokenDoesNotStartWithBearer()
        throws ServletException, IOException {
      // given
      request.addHeader("Authorization", "InvalidPrefix token");

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      verify(jwtTokenProvider, never()).validateAccessToken(anyString());
      verify(userDetailsService, never()).loadUserByUsername(anyString());
      verify(filterChain).doFilter(request, response);

      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("만료된 토큰일 때 에러 응답을 작성하고 필터 체인을 중단한다")
    void shouldWriteErrorResponseWhenTokenIsExpired() throws ServletException, IOException {
      // given
      String token = "expired.jwt.token";
      request.addHeader("Authorization", "Bearer " + token);

      BusinessException exception = new BusinessException(AuthErrorCode.EXPIRED_TOKEN);
      willThrow(exception).given(jwtTokenProvider).validateAccessToken(token);

      given(objectMapper.writeValueAsString(any())).willReturn("{\"error\":\"expired\"}");

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      verify(jwtTokenProvider).validateAccessToken(token);
      verify(filterChain, never()).doFilter(request, response);

      assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("잘못된 형식의 토큰일 때 에러 응답을 작성하고 필터 체인을 중단한다")
    void shouldWriteErrorResponseWhenTokenIsMalformed() throws ServletException, IOException {
      // given
      String token = "malformed.token";
      request.addHeader("Authorization", "Bearer " + token);

      BusinessException exception = new BusinessException(AuthErrorCode.MALFORMED_TOKEN);
      willThrow(exception).given(jwtTokenProvider).validateAccessToken(token);

      given(objectMapper.writeValueAsString(any())).willReturn("{\"error\":\"malformed\"}");

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      verify(jwtTokenProvider).validateAccessToken(token);
      verify(filterChain, never()).doFilter(request, response);

      assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("빈 Authorization 헤더일 때 인증 없이 필터 체인을 계속 진행한다")
    void shouldContinueFilterChainWhenAuthorizationHeaderIsEmpty()
        throws ServletException, IOException {
      // given
      request.addHeader("Authorization", "");

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      verify(jwtTokenProvider, never()).validateAccessToken(anyString());
      verify(userDetailsService, never()).loadUserByUsername(anyString());
      verify(filterChain).doFilter(request, response);

      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Bearer 뒤에 공백만 있을 때 인증 없이 필터 체인을 계속 진행한다")
    void shouldContinueFilterChainWhenBearerIsFollowedBySpace()
        throws ServletException, IOException {
      // given
      request.addHeader("Authorization", "Bearer ");

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      verify(jwtTokenProvider, never()).validateAccessToken(anyString());
      verify(userDetailsService, never()).loadUserByUsername(anyString());
      verify(filterChain).doFilter(request, response);

      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
  }
}
