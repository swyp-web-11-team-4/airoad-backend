package com.swygbro.airoad.backend.auth.presentation.web;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.swygbro.airoad.backend.auth.application.AuthUseCase;
import com.swygbro.airoad.backend.auth.domain.dto.response.TokenResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.common.exception.CommonErrorCode;
import com.swygbro.airoad.backend.common.presentation.GlobalExceptionHandler;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("AuthController 테스트")
class AuthControllerTest {

  @Mock private AuthUseCase authUseCase;

  @InjectMocks private AuthController authController;

  private MockMvc mockMvc;

  private static final String VALID_REFRESH_TOKEN = "Bearer valid.refresh.token";
  private static final String INVALID_REFRESH_TOKEN = "Bearer invalid.refresh.token";
  private static final String EXPIRED_REFRESH_TOKEN = "Bearer expired.refresh.token";
  private static final String UNREGISTERED_REFRESH_TOKEN = "Bearer unregistered.refresh.token";

  private static final String NEW_ACCESS_TOKEN = "new.access.token";
  private static final String NEW_REFRESH_TOKEN = "new.refresh.token";
  private static final long ACCESS_TOKEN_EXPIRES_IN = 3600000L;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(authController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(
                new AuthenticationPrincipalArgumentResolver(), new HeaderArgumentResolver())
            .build();
  }

  /**
   * @Header 어노테이션을 처리하기 위한 커스텀 ArgumentResolver HTTP 헤더를 메서드 파라미터로 주입
   */
  private static class HeaderArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.hasParameterAnnotation(Header.class);
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory) {
      Header headerAnnotation = parameter.getParameterAnnotation(Header.class);
      if (headerAnnotation != null) {
        String headerName = headerAnnotation.value();
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request != null) {
          return request.getHeader(headerName);
        }
      }
      return null;
    }
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("사용자가 유효한 리프레시 토큰으로 재발급 요청하면")
  class ReissueWithValidToken {

    @Test
    @DisplayName("200 OK와 새로운 토큰이 반환된다")
    void returnsNewTokens() throws Exception {
      // given - 유효한 리프레시 토큰과 새로운 토큰 응답 설정
      TokenResponse tokenResponse =
          TokenResponse.of(NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN, ACCESS_TOKEN_EXPIRES_IN);

      given(authUseCase.reissue(VALID_REFRESH_TOKEN)).willReturn(tokenResponse);

      // when - 토큰 재발급 요청 실행
      mockMvc
          .perform(
              post("/api/v1/auth/reissue")
                  .header("Authorization", VALID_REFRESH_TOKEN)
                  .contentType(MediaType.APPLICATION_JSON))
          .andDo(print())
          // then - 200 OK와 새로운 토큰 정보가 반환된다
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
          .andExpect(jsonPath("$.data.accessToken").value(NEW_ACCESS_TOKEN))
          .andExpect(jsonPath("$.data.refreshToken").value(NEW_REFRESH_TOKEN))
          .andExpect(jsonPath("$.data.accessTokenExpiresIn").value(ACCESS_TOKEN_EXPIRES_IN));

      // then - AuthUseCase의 reissue 메서드가 호출되었는지 검증
      verify(authUseCase).reissue(VALID_REFRESH_TOKEN);
    }
  }

  @Nested
  @DisplayName("사용자가 만료된 리프레시 토큰으로 재발급 요청하면")
  class ReissueWithExpiredToken {

    @Test
    @DisplayName("401 Unauthorized가 반환된다")
    void returnsUnauthorized() throws Exception {
      // given - 만료된 토큰 사용 시 예외 발생 설정
      willThrow(new BusinessException(CommonErrorCode.UNAUTHORIZED))
          .given(authUseCase)
          .reissue(EXPIRED_REFRESH_TOKEN);

      // when - 만료된 토큰으로 재발급 요청 실행
      mockMvc
          .perform(
              post("/api/v1/auth/reissue")
                  .header("Authorization", EXPIRED_REFRESH_TOKEN)
                  .contentType(MediaType.APPLICATION_JSON))
          .andDo(print())
          // then - 401 Unauthorized 에러가 반환된다
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
          .andExpect(jsonPath("$.data.code").value(CommonErrorCode.UNAUTHORIZED.getCode()))
          .andExpect(
              jsonPath("$.data.message").value(CommonErrorCode.UNAUTHORIZED.getDefaultMessage()));

      // then - AuthUseCase의 reissue 메서드가 호출되었는지 검증
      verify(authUseCase).reissue(EXPIRED_REFRESH_TOKEN);
    }
  }

  @Nested
  @DisplayName("사용자가 유효하지 않은 리프레시 토큰으로 재발급 요청하면")
  class ReissueWithInvalidToken {

    @Test
    @DisplayName("401 Unauthorized가 반환된다")
    void returnsUnauthorized() throws Exception {
      // given - 유효하지 않은 토큰 사용 시 예외 발생 설정
      willThrow(new BusinessException(CommonErrorCode.UNAUTHORIZED))
          .given(authUseCase)
          .reissue(INVALID_REFRESH_TOKEN);

      // when - 유효하지 않은 토큰으로 재발급 요청 실행
      mockMvc
          .perform(
              post("/api/v1/auth/reissue")
                  .header("Authorization", INVALID_REFRESH_TOKEN)
                  .contentType(MediaType.APPLICATION_JSON))
          .andDo(print())
          // then - 401 Unauthorized 에러가 반환된다
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
          .andExpect(jsonPath("$.data.code").value(CommonErrorCode.UNAUTHORIZED.getCode()));

      // then - AuthUseCase의 reissue 메서드가 호출되었는지 검증
      verify(authUseCase).reissue(INVALID_REFRESH_TOKEN);
    }
  }

  @Nested
  @DisplayName("사용자가 등록되지 않은 리프레시 토큰으로 재발급 요청하면")
  class ReissueWithUnregisteredToken {

    @Test
    @DisplayName("401 Unauthorized가 반환된다")
    void returnsUnauthorized() throws Exception {
      // given - 등록되지 않은 토큰 사용 시 예외 발생 설정
      willThrow(new BusinessException(CommonErrorCode.UNAUTHORIZED))
          .given(authUseCase)
          .reissue(UNREGISTERED_REFRESH_TOKEN);

      // when - 등록되지 않은 토큰으로 재발급 요청 실행
      mockMvc
          .perform(
              post("/api/v1/auth/reissue")
                  .header("Authorization", UNREGISTERED_REFRESH_TOKEN)
                  .contentType(MediaType.APPLICATION_JSON))
          .andDo(print())
          // then - 401 Unauthorized 에러가 반환된다
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
          .andExpect(jsonPath("$.data.code").value(CommonErrorCode.UNAUTHORIZED.getCode()))
          .andExpect(
              jsonPath("$.data.message").value(CommonErrorCode.UNAUTHORIZED.getDefaultMessage()));

      // then - AuthUseCase의 reissue 메서드가 호출되었는지 검증
      verify(authUseCase).reissue(UNREGISTERED_REFRESH_TOKEN);
    }
  }

  @Nested
  @DisplayName("인증된 사용자가 로그아웃 요청하면")
  class LogoutRequest {

    @Test
    @DisplayName("204 NO_CONTENT가 반환되고 응답 본문이 비어있다")
    void returnsOkWithEmptyBody() throws Exception {
      // given - 인증된 사용자 설정
      String accessToken = "Bearer " + "test-access-token";

      // when - 로그아웃 요청 실행
      mockMvc
          .perform(
              post("/api/v1/auth/logout")
                  .header("Authorization", accessToken)
                  .contentType(MediaType.APPLICATION_JSON))
          .andDo(print())
          // then - 204 NO_CONTENT가 반환되고 응답 본문이 비어있다
          .andExpect(status().isNoContent());

      // then - AuthUseCase의 logout 메서드가 호출되었는지 검증
      verify(authUseCase).logout(accessToken);
    }
  }
}
