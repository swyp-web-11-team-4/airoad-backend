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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserDetailsServiceImplTest {

  @Mock private MemberRepository memberRepository;

  @InjectMocks private UserDetailsServiceImpl userDetailsService;

  private Member testMember;

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
  }

  @Nested
  @DisplayName("loadUserByUsername 메서드는")
  class LoadUserByUsername {

    @Test
    @DisplayName("이메일로 회원을 찾아 UserDetails를 반환한다")
    void shouldReturnUserDetailsWhenMemberExists() {
      // given
      String email = "test@example.com";
      given(memberRepository.findByEmail(email)).willReturn(Optional.of(testMember));

      // when
      UserDetails userDetails = userDetailsService.loadUserByUsername(email);

      // then
      assertThat(userDetails).isNotNull();
      assertThat(userDetails.getUsername()).isEqualTo(email);
      assertThat(userDetails.getAuthorities()).hasSize(1);
      assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
          .isEqualTo("ROLE_MEMBER");
      verify(memberRepository).findByEmail(email);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회 시 UsernameNotFoundException을 발생시킨다")
    void shouldThrowUsernameNotFoundExceptionWhenMemberNotFound() {
      // given
      String email = "notfound@example.com";
      given(memberRepository.findByEmail(email)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
          .isInstanceOf(UsernameNotFoundException.class)
          .hasMessageContaining("사용자를 찾을 수 없습니다")
          .hasMessageContaining(email);
      verify(memberRepository).findByEmail(email);
    }

    @Test
    @DisplayName("ADMIN 권한을 가진 회원의 UserDetails를 반환한다")
    void shouldReturnUserDetailsForAdminMember() {
      // given
      String email = "admin@example.com";
      Member adminMember =
          Member.builder()
              .email(email)
              .name("Admin User")
              .imageUrl("https://example.com/admin.jpg")
              .provider(ProviderType.GOOGLE)
              .role(MemberRole.ADMIN)
              .build();
      given(memberRepository.findByEmail(email)).willReturn(Optional.of(adminMember));

      // when
      UserDetails userDetails = userDetailsService.loadUserByUsername(email);

      // then
      assertThat(userDetails).isNotNull();
      assertThat(userDetails.getUsername()).isEqualTo(email);
      assertThat(userDetails.getAuthorities()).hasSize(1);
      assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
          .isEqualTo("ROLE_ADMIN");
      verify(memberRepository).findByEmail(email);
    }
  }
}
