package com.swygbro.airoad.backend.auth.application;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.swygbro.airoad.backend.auth.domain.entity.RefreshToken;
import com.swygbro.airoad.backend.auth.exception.AuthErrorCode;
import com.swygbro.airoad.backend.auth.infrastructure.RefreshTokenRepository;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class TokenServiceTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @Mock private MemberRepository memberRepository;

  @InjectMocks private TokenService tokenService;

  private Member testMember;
  private RefreshToken testRefreshToken;

  @BeforeEach
  void setUp() {
    testMember =
        Member.builder()
            .email("test@example.com")
            .name("Test User")
            .imageUrl("https://example.com/image.jpg")
            .provider(ProviderType.GOOGLE)
            .role(MemberRole.MEMBER)
            .build();

    testRefreshToken =
        RefreshToken.builder()
            .token("refresh.token.value")
            .member(testMember)
            .expiryDate(java.time.LocalDateTime.now().plusDays(7))
            .build();

    // Set the refresh token expiration time
    ReflectionTestUtils.setField(tokenService, "refreshTokenExpiration", 604800000L); // 7 days
  }

  @Nested
  @DisplayName("createRefreshToken 메서드는")
  class CreateRefreshToken {

    @Test
    @DisplayName("이메일로 회원을 찾아 리프레시 토큰을 생성하고 저장한다")
    void shouldCreateAndSaveRefreshToken() {
      // given
      String token = "new.refresh.token";
      String email = "test@example.com";
      given(memberRepository.findByEmail(email)).willReturn(Optional.of(testMember));

      // when
      tokenService.createRefreshToken(token, email);

      // then
      verify(memberRepository).findByEmail(email);
      verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 토큰 생성 시 BusinessException을 발생시킨다")
    void shouldThrowBusinessExceptionWhenMemberNotFound() {
      // given
      String token = "new.refresh.token";
      String email = "notfound@example.com";
      given(memberRepository.findByEmail(email)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> tokenService.createRefreshToken(token, email))
          .isInstanceOf(BusinessException.class)
          .hasMessage(AuthErrorCode.MEMBER_NOT_FOUND.getDefaultMessage());
      verify(memberRepository).findByEmail(email);
    }
  }

  @Nested
  @DisplayName("deleteRefreshTokenByEmail 메서드는")
  class DeleteRefreshTokenByEmail {

    @Test
    @DisplayName("이메일로 리프레시 토큰을 찾아 삭제한다")
    void shouldDeleteRefreshTokenWhenExists() {
      // given
      String email = "test@example.com";
      given(refreshTokenRepository.findByMemberEmail(email))
          .willReturn(Optional.of(testRefreshToken));

      // when
      tokenService.deleteRefreshTokenByEmail(email);

      // then
      verify(refreshTokenRepository).findByMemberEmail(email);
      verify(refreshTokenRepository).delete(testRefreshToken);
    }

    @Test
    @DisplayName("리프레시 토큰이 존재하지 않을 때 아무 작업도 수행하지 않는다")
    void shouldDoNothingWhenRefreshTokenNotFound() {
      // given
      String email = "notfound@example.com";
      given(refreshTokenRepository.findByMemberEmail(email)).willReturn(Optional.empty());

      // when
      tokenService.deleteRefreshTokenByEmail(email);

      // then
      verify(refreshTokenRepository).findByMemberEmail(email);
      verify(refreshTokenRepository, org.mockito.Mockito.never()).delete(any(RefreshToken.class));
    }
  }
}
