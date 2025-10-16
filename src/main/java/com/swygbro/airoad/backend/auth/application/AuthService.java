package com.swygbro.airoad.backend.auth.application;

import java.util.Base64;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swygbro.airoad.backend.auth.domain.dto.LoginResponse;
import com.swygbro.airoad.backend.auth.domain.dto.TokenResponse;
import com.swygbro.airoad.backend.auth.domain.entity.TokenType;
import com.swygbro.airoad.backend.auth.domain.info.GoogleUserInfo;
import com.swygbro.airoad.backend.auth.domain.info.OAuth2UserInfo;
import com.swygbro.airoad.backend.auth.domain.principal.UserPrincipal;
import com.swygbro.airoad.backend.auth.filter.JwtTokenProvider;
import com.swygbro.airoad.backend.member.domain.dto.MemberResponse;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService implements AuthUseCase {

  private final MemberRepository memberRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final TokenService tokenService;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public LoginResponse socialLogin(ProviderType provider, String codeOrToken) {
    // 1. Provider API 호출하여 사용자 정보 가져오기
    Map<String, Object> attributes = fetchUserAttributes(provider, codeOrToken);

    // 2. OAuth2UserInfo 객체 생성
    OAuth2UserInfo userInfo = createOAuth2UserInfo(provider, attributes);

    // 3. 이메일 검증
    validateEmail(userInfo.getEmail(), provider);

    // 4. 회원 조회 또는 생성
    Member member = findOrCreateMember(userInfo, provider);

    // 5. Authentication 객체 생성
    Authentication authentication = createAuthentication(member, attributes);

    // 6. 기존 리프레시 토큰 삭제
    tokenService.deleteRefreshToken(member.getEmail());

    // 7. 새 토큰 발급
    TokenResponse tokenInfo = jwtTokenProvider.generateToken(authentication);

    // 8. 응답 생성
    MemberResponse memberInfo = MemberResponse.from(member);
    return LoginResponse.of(memberInfo, tokenInfo);
  }

  private Map<String, Object> fetchUserAttributes(ProviderType provider, String codeOrToken) {
    if (provider != ProviderType.GOOGLE) {
      throw new RuntimeException("지원하지 않는 Provider입니다: " + provider);
    }
    return parseGoogleIdToken(codeOrToken);
  }

  private Map<String, Object> parseGoogleIdToken(String idToken) {
    try {
      String[] parts = idToken.split("\\.");
      if (parts.length != 3) {
        throw new IllegalArgumentException("Invalid JWT token format");
      }

      String payload = parts[1];
      byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
      String decodedString = new String(decodedBytes);

      log.info("Decoded Google ID Token payload: {}", decodedString);

      Map<String, Object> attributes = objectMapper.readValue(decodedString, Map.class);

      if (!attributes.containsKey("email") || !attributes.containsKey("sub")) {
        throw new IllegalArgumentException("Invalid Google ID Token: missing required fields");
      }
      return attributes;

    } catch (Exception e) {
      throw new RuntimeException("Google ID Token 파싱 실패: " + e.getMessage());
    }
  }

  @Override
  @Transactional(readOnly = true)
  public TokenResponse reissueToken(String refreshToken) {
    if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
      throw new RuntimeException("유효하지 않은 리프레시 토큰");
    }

    Authentication authentication =
        jwtTokenProvider.getAuthentication(refreshToken, TokenType.REFRESH_TOKEN);

    tokenService.deleteRefreshToken(authentication.getName());

    return jwtTokenProvider.generateToken(authentication);
  }

  @Override
  @Transactional(readOnly = true)
  public void logout(String refreshToken) {
    String username = jwtTokenProvider.getUsernameFromToken(refreshToken, TokenType.REFRESH_TOKEN);

    tokenService.deleteRefreshToken(username);
  }

  private OAuth2UserInfo createOAuth2UserInfo(
      ProviderType provider, Map<String, Object> attributes) {
    return switch (provider) {
      case GOOGLE -> new GoogleUserInfo(attributes);
    };
  }

  private void validateEmail(String email, ProviderType provider) {
    if (!StringUtils.hasText(email)) {
      throw new OAuth2AuthenticationException(
          String.format("%s 이메일을 찾을 수 없습니다.", provider.getDisplayName()));
    }
  }

  private Member findOrCreateMember(OAuth2UserInfo userInfo, ProviderType provider) {
    return memberRepository
        .findByEmailAndProvider(userInfo.getEmail(), provider)
        .orElseGet(
            () -> {
              Member newMember =
                  Member.builder()
                      .email(userInfo.getEmail())
                      .name(userInfo.getName())
                      .imageUrl(userInfo.getImageUrl())
                      .provider(provider)
                      .role(MemberRole.MEMBER)
                      .build();

              log.info(
                  "[신규 회원 가입] email: {}, provider: {}",
                  userInfo.getEmail(),
                  provider.getProviderName());

              return memberRepository.save(newMember);
            });
  }

  private Authentication createAuthentication(Member member, Map<String, Object> attributes) {
    UserPrincipal userPrincipal = UserPrincipal.create(member, attributes);
    return new UsernamePasswordAuthenticationToken(
        userPrincipal, null, userPrincipal.getAuthorities());
  }
}
