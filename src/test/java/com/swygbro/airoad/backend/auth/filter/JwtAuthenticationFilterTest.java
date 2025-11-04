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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.auth.application.JwtTokenProvider;
import com.swygbro.airoad.backend.auth.application.UserDetailsServiceImpl;
import com.swygbro.airoad.backend.auth.domain.dto.UserPrincipal;
import com.swygbro.airoad.backend.auth.infrastructure.RefreshTokenRepository;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 테스트")
class JwtAuthenticationFilterTest {

  @Mock private JwtTokenProvider jwtTokenProvider;

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @Mock private UserDetailsServiceImpl userDetailsService;

  @Mock private FilterChain filterChain;

  @InjectMocks private JwtAuthenticationFilter jwtAuthenticationFilter;

  private static final String TEST_EMAIL = "test@example.com";
  private static final String VALID_TOKEN = "valid.jwt.token";
  private static final String INVALID_TOKEN = "invalid.jwt.token";

  @BeforeEach
  void setUp() {
    // 각 테스트 전에 SecurityContext를 초기화
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("사용자가 유효한 토큰으로 요청하면")
  class ValidTokenRequest {

    @Test
    @DisplayName("인증 정보가 설정되어 보호된 리소스에 접근할 수 있다")
    void authenticationIsSetForProtectedResources() throws ServletException, IOException {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();
      request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

      Member member = MemberFixture.createWithEmail(TEST_EMAIL);
      UserDetails userDetails = new UserPrincipal(member);

      given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
      given(jwtTokenProvider.getEmailFromToken(VALID_TOKEN)).willReturn(TEST_EMAIL);
      given(refreshTokenRepository.existsByEmail(TEST_EMAIL)).willReturn(true);
      given(userDetailsService.loadUserByUsername(TEST_EMAIL)).willReturn(userDetails);

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
      assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
          .isEqualTo(userDetails);
      assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
      verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 접두사 뒤의 토큰이 정확히 추출되어 검증된다")
    void extractsAndValidatesTokenAfterBearerPrefix() throws ServletException, IOException {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();
      request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

      Member member = MemberFixture.createWithEmail(TEST_EMAIL);
      UserDetails userDetails = new UserPrincipal(member);

      given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
      given(jwtTokenProvider.getEmailFromToken(VALID_TOKEN)).willReturn(TEST_EMAIL);
      given(refreshTokenRepository.existsByEmail(TEST_EMAIL)).willReturn(true);
      given(userDetailsService.loadUserByUsername(TEST_EMAIL)).willReturn(userDetails);

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      verify(jwtTokenProvider).validateToken(VALID_TOKEN);
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }
  }

  @Nested
  @DisplayName("사용자가 로그아웃 상태에서 토큰으로 요청하면")
  class LoggedOutUserRequest {

    @Test
    @DisplayName("리프레시 토큰이 없어 인증이 거부된다")
    void authenticationIsRejectedWithoutRefreshToken() throws ServletException, IOException {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();
      request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

      given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
      given(jwtTokenProvider.getEmailFromToken(VALID_TOKEN)).willReturn(TEST_EMAIL);
      given(refreshTokenRepository.existsByEmail(TEST_EMAIL)).willReturn(false);

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
      verify(userDetailsService, never()).loadUserByUsername(any());
      verify(filterChain).doFilter(request, response);
    }
  }

  @Nested
  @DisplayName("사용자가 토큰 없이 요청하면")
  class RequestWithoutToken {

    @Test
    @DisplayName("Authorization 헤더가 없어도 다음 필터로 진행된다")
    void proceedsWithoutAuthorizationHeader() throws ServletException, IOException {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();
      // Authorization 헤더 없음

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
      verify(jwtTokenProvider, never()).validateToken(any());
      verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 형식이 아니면 토큰으로 인식되지 않는다")
    void nonBearerTokenIsIgnored() throws ServletException, IOException {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();
      request.addHeader("Authorization", "Basic " + VALID_TOKEN); // Bearer가 아닌 Basic

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
      verify(jwtTokenProvider, never()).validateToken(any());
      verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 뒤에 토큰이 없으면 인증이 진행되지 않는다")
    void emptyTokenAfterBearerIsIgnored() throws ServletException, IOException {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();
      request.addHeader("Authorization", "Bearer "); // 토큰 없음

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
      verify(filterChain).doFilter(request, response);
    }
  }

  @Nested
  @DisplayName("사용자가 유효하지 않은 토큰으로 요청하면")
  class InvalidTokenRequest {

    @Test
    @DisplayName("인증 없이 다음 필터로 진행된다")
    void proceedsWithoutAuthentication() throws ServletException, IOException {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();
      request.addHeader("Authorization", "Bearer " + INVALID_TOKEN);

      given(jwtTokenProvider.validateToken(INVALID_TOKEN)).willReturn(false);

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
      verify(jwtTokenProvider, never()).getEmailFromToken(any());
      verify(filterChain).doFilter(request, response);
    }
  }

  @Nested
  @DisplayName("인증 처리 중 오류가 발생하면")
  class AuthenticationError {

    @Test
    @DisplayName("예외가 발생해도 애플리케이션은 정상 동작한다")
    void applicationContinuesOnException() throws ServletException, IOException {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();
      request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

      given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
      given(jwtTokenProvider.getEmailFromToken(VALID_TOKEN)).willReturn(TEST_EMAIL);
      given(refreshTokenRepository.existsByEmail(TEST_EMAIL))
          .willThrow(new RuntimeException("Database error"));

      // when & then
      assertThatCode(() -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain))
          .doesNotThrowAnyException();

      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
      verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("사용자 정보 조회 실패 시에도 필터 체인이 계속된다")
    void continuesWhenUserDetailsLoadFails() throws ServletException, IOException {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();
      request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

      given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
      given(jwtTokenProvider.getEmailFromToken(VALID_TOKEN)).willReturn(TEST_EMAIL);
      given(refreshTokenRepository.existsByEmail(TEST_EMAIL)).willReturn(true);
      given(userDetailsService.loadUserByUsername(TEST_EMAIL))
          .willThrow(new RuntimeException("User not found"));

      // when & then
      assertThatCode(() -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain))
          .doesNotThrowAnyException();

      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
      verify(filterChain).doFilter(request, response);
    }
  }
}
