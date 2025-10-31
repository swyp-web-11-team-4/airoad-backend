package com.swygbro.airoad.backend.auth.application;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.swygbro.airoad.backend.auth.domain.info.UserPrincipal;
import com.swygbro.airoad.backend.auth.filter.JwtTokenProvider;
import com.swygbro.airoad.backend.auth.util.CookieUtils;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;

import static org.mockito.BDDMockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc(addFilters = false)
class OAuthLoginSuccessHandlerTest {

  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private TokenService tokenService;
  @Mock private CookieUtils cookieUtils;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private RedirectStrategy redirectStrategy;
  @Mock private OAuth2AuthenticationToken authentication;
  @InjectMocks private OAuthLoginSuccessHandler handler;

  private static final String REDIRECT_URL = "http://localhost:3000";
  private static final String ACCESS_TOKEN_REDIRECT_URL =
      "%s/login?accessToken=%s&refreshToken=%s&tokenType=%s";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(handler, "ACCESS_TOKEN_REDIRECT_URI", ACCESS_TOKEN_REDIRECT_URL);
    ReflectionTestUtils.setField(handler, "redirectUri", REDIRECT_URL);
    handler.setRedirectStrategy(redirectStrategy);
  }

  @Nested
  @DisplayName("onAuthenticationSuccess 메서드는")
  class OnAuthenticationSuccess {

    @Test
    @DisplayName("사용자가 Google 로그인 시 토큰을 발급하고 리다이렉트한다")
    void shouldIssueTokensAndRedirect() throws Exception {
      // given
      String email = "test@gmail.com";
      String name = "Test User";
      String imageUrl = "https://example.com/image.jpg";
      String accessToken = "access-token";
      String refreshToken = "refresh-token";

      Member member =
          Member.builder()
              .email(email)
              .name(name)
              .imageUrl(imageUrl)
              .provider(ProviderType.GOOGLE)
              .role(MemberRole.MEMBER)
              .build();

      UserPrincipal userPrincipal = new UserPrincipal(member);

      given(authentication.getPrincipal()).willReturn(userPrincipal);
      given(jwtTokenProvider.generateAccessToken(email)).willReturn(accessToken);
      given(jwtTokenProvider.generateRefreshToken(email)).willReturn(refreshToken);

      // when
      handler.onAuthenticationSuccess(request, response, authentication);

      // then
      verify(jwtTokenProvider).generateAccessToken(email);
      verify(jwtTokenProvider).generateRefreshToken(email);
      verify(tokenService).createRefreshToken(refreshToken, email);
      verify(redirectStrategy)
          .sendRedirect(eq(request), eq(response), contains("/login?accessToken=" + accessToken));
    }
  }
}
