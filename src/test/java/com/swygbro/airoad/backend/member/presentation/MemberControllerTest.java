package com.swygbro.airoad.backend.member.presentation;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.swygbro.airoad.backend.auth.domain.dto.UserPrincipal;
import com.swygbro.airoad.backend.common.presentation.GlobalExceptionHandler;
import com.swygbro.airoad.backend.member.application.MemberUseCase;
import com.swygbro.airoad.backend.member.domain.dto.MemberResponse;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class MemberControllerTest {

  @Mock private MemberUseCase memberUseCase;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new MemberController(memberUseCase))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("getCurrentMember 메서드는")
  class GetCurrentMember {

    @Test
    @DisplayName("인증된 사용자의 정보를 조회하여 반환한다")
    void shouldReturnCurrentMemberInfo() throws Exception {
      // given
      String email = "test@naver.com";
      String name = "testName";
      String imageUrl = "https://example.com/image.jpg";
      String provider = "GOOGLE";
      String role = "MEMBER";

      Member member =
          Member.builder()
              .email(email)
              .name(name)
              .imageUrl(imageUrl)
              .provider(ProviderType.GOOGLE)
              .role(MemberRole.MEMBER)
              .build();

      UserPrincipal userPrincipal = new UserPrincipal(member);

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              userPrincipal, null, userPrincipal.getAuthorities());

      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);

      MemberResponse expectedResponse =
          MemberResponse.builder()
              .id(1L)
              .email(email)
              .name(name)
              .imageUrl(imageUrl)
              .provider(provider)
              .role(role)
              .build();

      given(memberUseCase.getMemberByEmail(email)).willReturn(expectedResponse);

      // when & then
      mockMvc
          .perform(get("/api/v1/members/me"))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
          .andExpect(jsonPath("$.data.email").value(email))
          .andExpect(jsonPath("$.data.name").value(name))
          .andExpect(jsonPath("$.data.imageUrl").value(imageUrl))
          .andExpect(jsonPath("$.data.provider").value(provider))
          .andExpect(jsonPath("$.data.role").value(role));

      verify(memberUseCase).getMemberByEmail(email);
    }
  }
}
