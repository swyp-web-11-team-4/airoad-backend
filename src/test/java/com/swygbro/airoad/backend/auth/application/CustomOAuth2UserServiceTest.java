package com.swygbro.airoad.backend.auth.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.auth.domain.info.UserPrincipal;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class CustomOAuth2UserServiceTest {

  @Mock private MemberRepository memberRepository;

  @InjectMocks private CustomOAuth2UserService customOAuth2UserService;

  private OAuth2UserRequest userRequest;
  private OAuth2User oAuth2User;
  private Map<String, Object> attributes;
  private Member existingMember;

  @BeforeEach
  void setUp() {
    // Google OAuth2 사용자 속성 설정
    attributes = new HashMap<>();
    attributes.put("sub", "google-user-id");
    attributes.put("name", "Test User");
    attributes.put("email", "test@example.com");
    attributes.put("picture", "https://example.com/profile.jpg");

    // OAuth2User 생성
    oAuth2User =
        new DefaultOAuth2User(
            java.util.Collections.singleton(new OAuth2UserAuthority(attributes)),
            attributes,
            "sub");

    // ClientRegistration 생성
    ClientRegistration clientRegistration =
        ClientRegistration.withRegistrationId("google")
            .clientId("test-client-id")
            .clientSecret("test-client-secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8080/login/oauth2/code/google")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://www.googleapis.com/oauth2/v4/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
            .userNameAttributeName("sub")
            .build();

    // OAuth2AccessToken 생성
    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "test-access-token",
            java.time.Instant.now(),
            java.time.Instant.now().plusSeconds(3600));

    userRequest = new OAuth2UserRequest(clientRegistration, accessToken);

    // 기존 회원 설정
    existingMember =
        Member.builder()
            .email("test@example.com")
            .name("Test User")
            .imageUrl("https://example.com/profile.jpg")
            .provider(ProviderType.GOOGLE)
            .role(MemberRole.MEMBER)
            .build();
  }

  @Nested
  @DisplayName("processOAuth2User 메서드는")
  class ProcessOAuth2User {

    @Test
    @DisplayName("기존 회원이 존재하면 UserPrincipal을 반환한다")
    void shouldReturnUserPrincipalWhenMemberExists() {
      // given
      given(memberRepository.findByEmailAndProvider("test@example.com", ProviderType.GOOGLE))
          .willReturn(Optional.of(existingMember));

      // when
      OAuth2User result = customOAuth2UserService.processOAuth2User(userRequest, oAuth2User);

      // then
      assertThat(result).isInstanceOf(UserPrincipal.class);
      UserPrincipal principal = (UserPrincipal) result;
      assertThat(principal.member()).isEqualTo(existingMember);
      assertThat(principal.getName()).isEqualTo("Test User");
      assertThat(principal.getUsername()).isEqualTo("test@example.com");

      verify(memberRepository).findByEmailAndProvider("test@example.com", ProviderType.GOOGLE);
      verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("신규 회원이면 회원을 생성하고 UserPrincipal을 반환한다")
    void shouldCreateNewMemberAndReturnUserPrincipalWhenMemberNotExists() {
      // given
      Member newMember =
          Member.builder()
              .email("test@example.com")
              .name("Test User")
              .imageUrl("https://example.com/profile.jpg")
              .provider(ProviderType.GOOGLE)
              .role(MemberRole.MEMBER)
              .build();

      given(memberRepository.findByEmailAndProvider("test@example.com", ProviderType.GOOGLE))
          .willReturn(Optional.empty());
      given(memberRepository.save(any(Member.class))).willReturn(newMember);

      // when
      OAuth2User result = customOAuth2UserService.processOAuth2User(userRequest, oAuth2User);

      // then
      assertThat(result).isInstanceOf(UserPrincipal.class);
      UserPrincipal principal = (UserPrincipal) result;
      assertThat(principal.member()).isEqualTo(newMember);
      assertThat(principal.getName()).isEqualTo("Test User");

      verify(memberRepository).findByEmailAndProvider("test@example.com", ProviderType.GOOGLE);
      verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("이메일이 없으면 OAuth2AuthenticationException을 발생시킨다")
    void shouldThrowExceptionWhenEmailIsEmpty() {
      // given
      attributes.put("email", "");
      OAuth2User oAuth2UserWithoutEmail =
          new DefaultOAuth2User(
              java.util.Collections.singleton(new OAuth2UserAuthority(attributes)),
              attributes,
              "sub");

      // when & then
      assertThatThrownBy(
              () -> customOAuth2UserService.processOAuth2User(userRequest, oAuth2UserWithoutEmail))
          .isInstanceOf(OAuth2AuthenticationException.class);

      verify(memberRepository, never()).findByEmailAndProvider(any(), any());
      verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("이메일이 null이면 OAuth2AuthenticationException을 발생시킨다")
    void shouldThrowExceptionWhenEmailIsNull() {
      // given
      attributes.remove("email");
      OAuth2User oAuth2UserWithoutEmail =
          new DefaultOAuth2User(
              java.util.Collections.singleton(new OAuth2UserAuthority(attributes)),
              attributes,
              "sub");

      // when & then
      assertThatThrownBy(
              () -> customOAuth2UserService.processOAuth2User(userRequest, oAuth2UserWithoutEmail))
          .isInstanceOf(OAuth2AuthenticationException.class);

      verify(memberRepository, never()).findByEmailAndProvider(any(), any());
      verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Google 제공자 정보를 올바르게 처리한다")
    void shouldProcessGoogleProvider() {
      // given
      given(memberRepository.findByEmailAndProvider("test@example.com", ProviderType.GOOGLE))
          .willReturn(Optional.of(existingMember));

      // when
      OAuth2User result = customOAuth2UserService.processOAuth2User(userRequest, oAuth2User);

      // then
      assertThat(result).isInstanceOf(UserPrincipal.class);
      verify(memberRepository).findByEmailAndProvider("test@example.com", ProviderType.GOOGLE);
    }
  }

  @Nested
  @DisplayName("createNewMember 메서드는")
  class CreateNewMember {

    @Test
    @DisplayName("OAuth2UserInfo로부터 새로운 회원을 생성한다")
    void shouldCreateNewMemberFromOAuth2UserInfo() {
      // given
      Member savedMember =
          Member.builder()
              .email("newuser@example.com")
              .name("New User")
              .imageUrl("https://example.com/new-profile.jpg")
              .provider(ProviderType.GOOGLE)
              .role(MemberRole.MEMBER)
              .build();

      Map<String, Object> newAttributes = new HashMap<>();
      newAttributes.put("sub", "google-user-id");
      newAttributes.put("email", "newuser@example.com");
      newAttributes.put("name", "New User");
      newAttributes.put("picture", "https://example.com/new-profile.jpg");

      OAuth2User newOAuth2User =
          new DefaultOAuth2User(
              java.util.Collections.singleton(new OAuth2UserAuthority(newAttributes)),
              newAttributes,
              "sub");

      given(memberRepository.findByEmailAndProvider("newuser@example.com", ProviderType.GOOGLE))
          .willReturn(Optional.empty());
      given(memberRepository.save(any(Member.class))).willReturn(savedMember);

      // when
      OAuth2User result = customOAuth2UserService.processOAuth2User(userRequest, newOAuth2User);

      // then
      assertThat(result).isInstanceOf(UserPrincipal.class);
      UserPrincipal principal = (UserPrincipal) result;
      assertThat(principal.member().getEmail()).isEqualTo("newuser@example.com");
      assertThat(principal.member().getName()).isEqualTo("New User");
      assertThat(principal.member().getImageUrl()).isEqualTo("https://example.com/new-profile.jpg");
      assertThat(principal.member().getProvider()).isEqualTo(ProviderType.GOOGLE);
      assertThat(principal.member().getRole()).isEqualTo(MemberRole.MEMBER);

      verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("새로운 회원은 기본적으로 MEMBER 역할을 가진다")
    void shouldAssignMemberRoleByDefault() {
      // given
      Member savedMember =
          Member.builder()
              .email("test@example.com")
              .name("Test User")
              .imageUrl("https://example.com/profile.jpg")
              .provider(ProviderType.GOOGLE)
              .role(MemberRole.MEMBER)
              .build();

      given(memberRepository.findByEmailAndProvider("test@example.com", ProviderType.GOOGLE))
          .willReturn(Optional.empty());
      given(memberRepository.save(any(Member.class))).willReturn(savedMember);

      // when
      OAuth2User result = customOAuth2UserService.processOAuth2User(userRequest, oAuth2User);

      // then
      UserPrincipal principal = (UserPrincipal) result;
      assertThat(principal.member().getRole()).isEqualTo(MemberRole.MEMBER);
    }
  }
}
