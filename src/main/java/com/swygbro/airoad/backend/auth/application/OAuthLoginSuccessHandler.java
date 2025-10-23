package com.swygbro.airoad.backend.auth.application;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.auth.domain.dto.Auth;
import com.swygbro.airoad.backend.auth.domain.info.GoogleUserInfo;
import com.swygbro.airoad.backend.auth.domain.info.OAuth2UserInfo;
import com.swygbro.airoad.backend.auth.filter.JwtTokenProvider;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  @Value("${app.oauth2.access}")
  private String ACCESS_TOKEN_REDIRECT_URI;

  @Value("${app.oauth2.register}")
  private String REGISTER_TOKEN_REDIRECT_URI;

  @Value("${app.oauth2.redirect-uri}")
  private String redirectUri;

  private final MemberRepository memberRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final TokenService tokenService;

  @Override
  @Transactional
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {

    OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
    String provider = oauthToken.getAuthorizedClientRegistrationId();
    OAuth2User oAuth2User = oauthToken.getPrincipal();

    OAuth2UserInfo userInfo = extractOAuth2UserInfo(provider, oAuth2User);
    ProviderType providerType = ProviderType.valueOf(provider.toUpperCase());

    handleAuthentication(request, response, userInfo, providerType);
  }

  private OAuth2UserInfo extractOAuth2UserInfo(String provider, OAuth2User oAuth2User) {
    if ("google".equalsIgnoreCase(provider)) {
      return new GoogleUserInfo(oAuth2User.getAttributes());
    }
    throw new IllegalArgumentException("지원하지 않는 OAuth2 제공자입니다: " + provider);
  }

  private void handleAuthentication(
      HttpServletRequest request,
      HttpServletResponse response,
      OAuth2UserInfo userInfo,
      ProviderType providerType)
      throws IOException {

    Auth auth =
        memberRepository
            .findByEmailAndProvider(userInfo.getEmail(), providerType)
            .map(member -> new Auth(member, false))
            .orElseGet(() -> new Auth(createNewMember(userInfo, providerType), true));

    redirectWithTokens(request, response, auth.member(), auth.isNew());
  }

  private Member createNewMember(OAuth2UserInfo userInfo, ProviderType providerType) {
    return memberRepository.save(
        Member.builder()
            .email(userInfo.getEmail())
            .name(userInfo.getName())
            .imageUrl(userInfo.getImageUrl())
            .provider(providerType)
            .role(MemberRole.MEMBER)
            .build());
  }

  private void redirectWithTokens(
      HttpServletRequest request, HttpServletResponse response, Member member, boolean isNewUser)
      throws IOException {

    String email = member.getEmail();
    String accessToken = jwtTokenProvider.generateAccessToken(email, member.getRole().name());
    String refreshToken = jwtTokenProvider.generateRefreshToken(email);
    
    if (!isNewUser) {
      tokenService.deleteRefreshTokenByEmail(email);
    }
    tokenService.createRefreshToken(refreshToken, email);

    String template = isNewUser ? REGISTER_TOKEN_REDIRECT_URI : ACCESS_TOKEN_REDIRECT_URI;
    String redirectUrl = String.format(template, redirectUri, accessToken, refreshToken);
    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
  }
}
