package com.swygbro.airoad.backend.member.application;

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

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.member.domain.dto.MemberResponse;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;
import com.swygbro.airoad.backend.member.exception.MemberErrorCode;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class MemberServiceTest {

  @Mock private MemberRepository memberRepository;

  @InjectMocks private MemberService memberService;

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
  @DisplayName("getMemberByEmail 메서드는")
  class GetMemberByEmail {

    @Test
    @DisplayName("이메일로 회원을 조회하여 MemberResponse를 반환한다")
    void shouldReturnMemberResponseByEmail() {
      // given
      String email = "test@example.com";
      given(memberRepository.findByEmail(email)).willReturn(Optional.of(testMember));

      // when
      MemberResponse response = memberService.getMemberByEmail(email);

      // then
      assertThat(response).isNotNull();
      assertThat(response.getEmail()).isEqualTo(email);
      assertThat(response.getName()).isEqualTo("Test User");
      assertThat(response.getImageUrl()).isEqualTo("https://example.com/image.jpg");
      assertThat(response.getProvider()).isEqualTo("google");
      assertThat(response.getRole()).isEqualTo("MEMBER");
      verify(memberRepository).findByEmail(email);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회 시 BusinessException을 발생시킨다")
    void shouldThrowBusinessExceptionWhenMemberNotFound() {
      // given
      String email = "notfound@example.com";
      given(memberRepository.findByEmail(email)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> memberService.getMemberByEmail(email))
          .isInstanceOf(BusinessException.class)
          .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getDefaultMessage());
      verify(memberRepository).findByEmail(email);
    }
  }
}
