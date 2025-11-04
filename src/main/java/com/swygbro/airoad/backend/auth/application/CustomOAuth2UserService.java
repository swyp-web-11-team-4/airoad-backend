package com.swygbro.airoad.backend.auth.application;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.swygbro.airoad.backend.auth.domain.dto.UserPrincipal;
import com.swygbro.airoad.backend.auth.domain.dto.oauth2.OAuth2UserInfo;
import com.swygbro.airoad.backend.auth.domain.dto.oauth2.OAuth2UserInfoFactory;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final MemberRepository memberRepository;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(userRequest);

    try {
      return processOAuth2User(userRequest, oAuth2User);
    } catch (Exception e) {
      log.error("Error processing OAuth2 user", e);
      throw new OAuth2AuthenticationException(e.getMessage());
    }
  }

  private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
    String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();

    OAuth2UserInfo oAuth2UserInfo =
        OAuth2UserInfoFactory.extractOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

    if (!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
      throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
    }

    ProviderType providerType = ProviderType.valueOf(registrationId);

    var member =
        memberRepository
            .findByEmail(oAuth2UserInfo.getEmail())
            .orElseGet(() -> createNewMember(oAuth2UserInfo, providerType));

    return new UserPrincipal(member);
  }

  private Member createNewMember(OAuth2UserInfo oAuth2UserInfo, ProviderType providerType) {
    Member newMember =
        Member.builder()
            .email(oAuth2UserInfo.getEmail())
            .name(oAuth2UserInfo.getName())
            .imageUrl(oAuth2UserInfo.getImageUrl())
            .provider(providerType)
            .role(MemberRole.MEMBER)
            .build();

    return memberRepository.save(newMember);
  }
}
