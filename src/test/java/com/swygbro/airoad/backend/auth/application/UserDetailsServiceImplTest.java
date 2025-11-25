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

import com.swygbro.airoad.backend.common.infrastructure.encryption.SHA256Hasher;
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

  @Mock private SHA256Hasher sha256Hasher;

  @InjectMocks private UserDetailsServiceImpl userDetailsService;

  private static final String TEST_EMAIL = "test@example.com";
  private static final String TEST_EMAIL_HASH = "hash_test@example.com";
  private static final String ADMIN_EMAIL = "admin@example.com";
  private static final String ADMIN_EMAIL_HASH = "hash_admin@example.com";
  private static final String NOT_FOUND_EMAIL = "notfound@example.com";
  private static final String NOT_FOUND_EMAIL_HASH = "hash_notfound@example.com";

  private Member testMember;

  @BeforeEach
  void setUp() {
    testMember =
        Member.builder()
            .email(TEST_EMAIL)
            .emailHash(TEST_EMAIL_HASH)
            .name("Test User")
            .nameHash("hash_test_user")
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
      given(sha256Hasher.hash(TEST_EMAIL)).willReturn(TEST_EMAIL_HASH);
      given(memberRepository.findByEmailHash(TEST_EMAIL_HASH)).willReturn(Optional.of(testMember));

      // when
      UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_EMAIL);

      // then
      assertThat(userDetails).isNotNull();
      assertThat(userDetails.getUsername()).isEqualTo(TEST_EMAIL);
      assertThat(userDetails.getAuthorities()).hasSize(1);
      assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
          .isEqualTo("ROLE_MEMBER");
      verify(memberRepository).findByEmailHash(TEST_EMAIL_HASH);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회 시 UsernameNotFoundException을 발생시킨다")
    void shouldThrowUsernameNotFoundExceptionWhenMemberNotFound() {
      // given
      given(sha256Hasher.hash(NOT_FOUND_EMAIL)).willReturn(NOT_FOUND_EMAIL_HASH);
      given(memberRepository.findByEmailHash(NOT_FOUND_EMAIL_HASH)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userDetailsService.loadUserByUsername(NOT_FOUND_EMAIL))
          .isInstanceOf(UsernameNotFoundException.class)
          .hasMessageContaining("사용자를 찾을 수 없습니다")
          .hasMessageContaining(NOT_FOUND_EMAIL);
      verify(memberRepository).findByEmailHash(NOT_FOUND_EMAIL_HASH);
    }

    @Test
    @DisplayName("ADMIN 권한을 가진 회원의 UserDetails를 반환한다")
    void shouldReturnUserDetailsForAdminMember() {
      // given
      Member adminMember =
          Member.builder()
              .email(ADMIN_EMAIL)
              .emailHash(ADMIN_EMAIL_HASH)
              .name("Admin User")
              .nameHash("hash_admin_user")
              .imageUrl("https://example.com/admin.jpg")
              .provider(ProviderType.GOOGLE)
              .role(MemberRole.ADMIN)
              .build();
      given(sha256Hasher.hash(ADMIN_EMAIL)).willReturn(ADMIN_EMAIL_HASH);
      given(memberRepository.findByEmailHash(ADMIN_EMAIL_HASH))
          .willReturn(Optional.of(adminMember));

      // when
      UserDetails userDetails = userDetailsService.loadUserByUsername(ADMIN_EMAIL);

      // then
      assertThat(userDetails).isNotNull();
      assertThat(userDetails.getUsername()).isEqualTo(ADMIN_EMAIL);
      assertThat(userDetails.getAuthorities()).hasSize(1);
      assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
          .isEqualTo("ROLE_ADMIN");
      verify(memberRepository).findByEmailHash(ADMIN_EMAIL_HASH);
    }
  }
}
