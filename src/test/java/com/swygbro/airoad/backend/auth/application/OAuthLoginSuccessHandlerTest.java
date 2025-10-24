package com.swygbro.airoad.backend.auth.application;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

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
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.swygbro.airoad.backend.auth.filter.JwtTokenProvider;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc(addFilters = false)
class OAuthLoginSuccessHandlerTest {

  @Mock private MemberRepository memberRepository;
  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private TokenService tokenService;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private RedirectStrategy redirectStrategy;
  @Mock private OAuth2AuthenticationToken authentication;
  @Mock private OAuth2User oAuth2User;
  @InjectMocks private OAuthLoginSuccessHandler handler;

  private static final String REDIRECT_URL = "http://localhost:3000";
  private static final String ACCESS_TOKEN_REDIRECT_URL = "%s/login?accessToken=%s&refreshToken=%s";
  private static final String REGISTER_TOKEN_REDIRECT_URL =
      "%s/register?accessToken=%s&refreshToken=%s";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(handler, "ACCESS_TOKEN_REDIRECT_URI", ACCESS_TOKEN_REDIRECT_URL);
    ReflectionTestUtils.setField(
        handler, "REGISTER_TOKEN_REDIRECT_URI", REGISTER_TOKEN_REDIRECT_URL);
    ReflectionTestUtils.setField(handler, "redirectUri", REDIRECT_URL);
    handler.setRedirectStrategy(redirectStrategy);
  }

  @Nested
  @DisplayName("onAuthenticationSuccess 메서드는")
  class OnAuthenticationSuccess {

    @Test
    @DisplayName("신규 사용자가 Google 로그인 시 회원을 생성하고 토큰을 발급한다")
    void shouldCreateNewMemberAndIssueTokensForNewUser() throws IOException {
      // given
      String email = "test@gmail.com";
      String name = "Test User";
      String imageUrl = "https://example.com/image.jpg";
      String accessToken = "access-token";
      String refreshToken = "refresh-token";

      Map<String, Object> attributes =
          Map.of(
              "email", email,
              "name", name,
              "picture", imageUrl);

      given(authentication.getAuthorizedClientRegistrationId()).willReturn("google");
      given(authentication.getPrincipal()).willReturn(oAuth2User);
      given(oAuth2User.getAttributes()).willReturn(attributes);
      given(memberRepository.findByEmailAndProvider(email, ProviderType.GOOGLE))
          .willReturn(Optional.empty());

      Member newMember =
          Member.builder()
              .email(email)
              .name(name)
              .imageUrl(imageUrl)
              .provider(ProviderType.GOOGLE)
              .role(MemberRole.MEMBER)
              .build();
      given(memberRepository.save(any(Member.class))).willReturn(newMember);
      given(jwtTokenProvider.generateAccessToken(email, MemberRole.MEMBER.name()))
          .willReturn(accessToken);
      given(jwtTokenProvider.generateRefreshToken(email)).willReturn(refreshToken);

      // when
      handler.onAuthenticationSuccess(request, response, authentication);

      // then
      verify(memberRepository).save(any(Member.class));
      verify(jwtTokenProvider).generateAccessToken(email, MemberRole.MEMBER.name());
      verify(jwtTokenProvider).generateRefreshToken(email);
      verify(tokenService).createRefreshToken(refreshToken, email);
      verify(redirectStrategy)
          .sendRedirect(
              eq(request), eq(response), contains("/register?accessToken=" + accessToken));
    }

    @Test
    @DisplayName("기존 사용자가 Google 로그인 시 기존 토큰을 삭제하고 새 토큰을 발급한다")
    void shouldDeleteOldTokenAndIssueNewTokensForExistingUser() throws IOException {
      // given
      String email = "existing@gmail.com";
      String name = "Existing User";
      String imageUrl = "https://example.com/existing.jpg";
      String accessToken = "new-access-token";
      String refreshToken = "new-refresh-token";

      Map<String, Object> attributes =
          Map.of(
              "email", email,
              "name", name,
              "picture", imageUrl);

      Member existingMember =
          Member.builder()
              .email(email)
              .name(name)
              .imageUrl(imageUrl)
              .provider(ProviderType.GOOGLE)
              .role(MemberRole.MEMBER)
              .build();

      given(authentication.getAuthorizedClientRegistrationId()).willReturn("google");
      given(authentication.getPrincipal()).willReturn(oAuth2User);
      given(oAuth2User.getAttributes()).willReturn(attributes);
      given(memberRepository.findByEmailAndProvider(email, ProviderType.GOOGLE))
          .willReturn(Optional.of(existingMember));
      given(jwtTokenProvider.generateAccessToken(email, MemberRole.MEMBER.name()))
          .willReturn(accessToken);
      given(jwtTokenProvider.generateRefreshToken(email)).willReturn(refreshToken);

      // when
      handler.onAuthenticationSuccess(request, response, authentication);

      // then
      verify(tokenService).deleteRefreshTokenByEmail(email);
      verify(tokenService).createRefreshToken(refreshToken, email);
      verify(jwtTokenProvider).generateAccessToken(email, MemberRole.MEMBER.name());
      verify(jwtTokenProvider).generateRefreshToken(email);
      verify(redirectStrategy)
          .sendRedirect(eq(request), eq(response), contains("/login?accessToken=" + accessToken));
    }

    @Test
    @DisplayName("지원하지 않는 OAuth2 제공자로 로그인 시도 시 예외를 발생시킨다")
    void shouldThrowExceptionForUnsupportedProvider() {
      // given
      given(authentication.getAuthorizedClientRegistrationId()).willReturn("kakao");
      given(authentication.getPrincipal()).willReturn(oAuth2User);

      // when & then
      assertThatThrownBy(() -> handler.onAuthenticationSuccess(request, response, authentication))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("지원하지 않는 OAuth2 제공자입니다");
    }
  }
}
