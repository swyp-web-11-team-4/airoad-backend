package com.swygbro.airoad.backend.auth.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swygbro.airoad.backend.auth.application.AuthUseCase;
import com.swygbro.airoad.backend.auth.domain.dto.RefreshTokenRequest;
import com.swygbro.airoad.backend.auth.domain.dto.TokenResponse;
import com.swygbro.airoad.backend.auth.exception.AuthErrorCode;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.common.presentation.GlobalExceptionHandler;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthControllerTest {

  @InjectMocks private AuthController authController;
  @Mock private AuthUseCase authUseCase;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(authController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    objectMapper = new ObjectMapper();
  }

  @Nested
  @DisplayName("reissueToken 메서드는")
  class ReissueToken {

    @Test
    @DisplayName("유효한 리프레시 토큰으로 새로운 액세스 토큰을 발급한다")
    void shouldReissueTokenWithValidRefreshToken() throws Exception {
      // given
      String newAccessToken = "new.access.token";
      String refreshToken = "valid.refresh.token";

      RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

      TokenResponse expectedResponse =
          TokenResponse.builder()
              .accessToken(newAccessToken)
              .refreshToken(refreshToken)
              .tokenType("Bearer")
              .build();

      given(authUseCase.reissue(refreshToken)).willReturn(expectedResponse);

      // when & then
      mockMvc
          .perform(
              post("/api/v1/auth/reissue")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(200))
          .andExpect(jsonPath("$.data.accessToken").value(newAccessToken))
          .andExpect(jsonPath("$.data.refreshToken").value(refreshToken))
          .andExpect(jsonPath("$.data.tokenType").value("Bearer"));

      verify(authUseCase).reissue(refreshToken);
    }

    @Test
    @DisplayName("만료된 리프레시 토큰으로 요청하면 UNAUTHORIZED를 반환한다")
    void shouldReturnUnauthorizedWhenTokenIsExpired() throws Exception {
      // given
      String expiredToken = "expired.refresh.token";
      RefreshTokenRequest request = new RefreshTokenRequest(expiredToken);

      given(authUseCase.reissue(expiredToken))
          .willThrow(new BusinessException(AuthErrorCode.EXPIRED_TOKEN));

      // when & then
      mockMvc
          .perform(
              post("/api/v1/auth/reissue")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(401))
          .andExpect(jsonPath("$.data.code").value("AUTH002"))
          .andExpect(jsonPath("$.data.message").value("만료된 토큰입니다."));

      verify(authUseCase).reissue(expiredToken);
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 요청하면 UNAUTHORIZED를 반환한다")
    void shouldReturnUnauthorizedWhenTokenIsInvalid() throws Exception {
      // given
      String invalidToken = "invalid.refresh.token";
      RefreshTokenRequest request = new RefreshTokenRequest(invalidToken);

      given(authUseCase.reissue(invalidToken))
          .willThrow(new BusinessException(AuthErrorCode.INVALID_TOKEN));

      // when & then
      mockMvc
          .perform(
              post("/api/v1/auth/reissue")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(401))
          .andExpect(jsonPath("$.data.code").value("AUTH001"))
          .andExpect(jsonPath("$.data.message").value("유효하지 않은 토큰입니다."));

      verify(authUseCase).reissue(invalidToken);
    }

    @Test
    @DisplayName("잘못된 형식의 리프레시 토큰으로 요청하면 UNAUTHORIZED를 반환한다")
    void shouldReturnUnauthorizedWhenTokenIsMalformed() throws Exception {
      // given
      String malformedToken = "malformed-token";
      RefreshTokenRequest request = new RefreshTokenRequest(malformedToken);

      given(authUseCase.reissue(malformedToken))
          .willThrow(new BusinessException(AuthErrorCode.MALFORMED_TOKEN));

      // when & then
      mockMvc
          .perform(
              post("/api/v1/auth/reissue")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(401))
          .andExpect(jsonPath("$.data.code").value("AUTH003"))
          .andExpect(jsonPath("$.data.message").value("잘못된 형식의 토큰입니다."));

      verify(authUseCase).reissue(malformedToken);
    }

    @Test
    @DisplayName("빈 토큰으로 요청하면 UNAUTHORIZED를 반환한다")
    void shouldReturnUnauthorizedWhenTokenIsEmpty() throws Exception {
      // given
      String emptyToken = "";
      RefreshTokenRequest request = new RefreshTokenRequest(emptyToken);

      given(authUseCase.reissue(emptyToken))
          .willThrow(new BusinessException(AuthErrorCode.EMPTY_TOKEN));

      // when & then
      mockMvc
          .perform(
              post("/api/v1/auth/reissue")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(401))
          .andExpect(jsonPath("$.data.code").value("AUTH005"))
          .andExpect(jsonPath("$.data.message").value("토큰이 비어있습니다."));

      verify(authUseCase).reissue(emptyToken);
    }
  }

  @Nested
  @DisplayName("logout 메서드는")
  class Logout {

    @Test
    @DisplayName("유효한 리프레시 토큰으로 로그아웃을 성공한다")
    void shouldLogoutSuccessfully() throws Exception {
      // given
      String refreshToken = "valid.refresh.token";
      RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

      willDoNothing().given(authUseCase).logout(refreshToken);

      // when & then
      mockMvc
          .perform(
              post("/api/v1/auth/logout")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").doesNotExist()); // ResponseEntity<Void>

      verify(authUseCase).logout(refreshToken);
    }

    @Test
    @DisplayName("만료된 리프레시 토큰으로 로그아웃 요청하면 UNAUTHORIZED를 반환한다")
    void shouldReturnUnauthorizedWhenTokenIsExpired() throws Exception {
      // given
      String expiredToken = "expired.refresh.token";
      RefreshTokenRequest request = new RefreshTokenRequest(expiredToken);

      willThrow(new BusinessException(AuthErrorCode.EXPIRED_TOKEN))
          .given(authUseCase)
          .logout(expiredToken);

      // when & then
      mockMvc
          .perform(
              post("/api/v1/auth/logout")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(401))
          .andExpect(jsonPath("$.data.code").value("AUTH002"))
          .andExpect(jsonPath("$.data.message").value("만료된 토큰입니다."));

      verify(authUseCase).logout(expiredToken);
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 로그아웃 요청하면 UNAUTHORIZED를 반환한다")
    void shouldReturnUnauthorizedWhenTokenIsInvalid() throws Exception {
      // given
      String invalidToken = "invalid.refresh.token";
      RefreshTokenRequest request = new RefreshTokenRequest(invalidToken);

      willThrow(new BusinessException(AuthErrorCode.INVALID_TOKEN))
          .given(authUseCase)
          .logout(invalidToken);

      // when & then
      mockMvc
          .perform(
              post("/api/v1/auth/logout")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(401))
          .andExpect(jsonPath("$.data.code").value("AUTH001"))
          .andExpect(jsonPath("$.data.message").value("유효하지 않은 토큰입니다."));

      verify(authUseCase).logout(invalidToken);
    }

    @Test
    @DisplayName("잘못된 형식의 리프레시 토큰으로 로그아웃 요청하면 UNAUTHORIZED를 반환한다")
    void shouldReturnUnauthorizedWhenTokenIsMalformed() throws Exception {
      // given
      String malformedToken = "malformed-token";
      RefreshTokenRequest request = new RefreshTokenRequest(malformedToken);

      willThrow(new BusinessException(AuthErrorCode.MALFORMED_TOKEN))
          .given(authUseCase)
          .logout(malformedToken);

      // when & then
      mockMvc
          .perform(
              post("/api/v1/auth/logout")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(401))
          .andExpect(jsonPath("$.data.code").value("AUTH003"))
          .andExpect(jsonPath("$.data.message").value("잘못된 형식의 토큰입니다."));

      verify(authUseCase).logout(malformedToken);
    }

    @Test
    @DisplayName("토큰이 이미 삭제된 경우에도 로그아웃이 정상 처리된다")
    void shouldHandleLogoutForAlreadyDeletedToken() throws Exception {
      // given
      String deletedToken = "already.deleted.token";
      RefreshTokenRequest request = new RefreshTokenRequest(deletedToken);

      willDoNothing().given(authUseCase).logout(deletedToken);

      // when & then
      mockMvc
          .perform(
              post("/api/v1/auth/logout")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").doesNotExist()); // ResponseEntity<Void>

      verify(authUseCase).logout(deletedToken);
    }
  }
}
