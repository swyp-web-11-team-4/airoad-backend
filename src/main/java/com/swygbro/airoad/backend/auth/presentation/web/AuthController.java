package com.swygbro.airoad.backend.auth.presentation.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swygbro.airoad.backend.auth.application.AuthUseCase;
import com.swygbro.airoad.backend.auth.domain.dto.response.TokenResponse;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "v1-auth", description = "회원 인증을 담당하는 API")
public class AuthController {

  private final AuthUseCase authUseCase;

  @PostMapping("/reissue")
  public ResponseEntity<CommonResponse<TokenResponse>> reissueToken(
      @Header("Authorization") String refreshToken) {
    TokenResponse tokenResponse = authUseCase.reissue(refreshToken);

    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, tokenResponse));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Header("Authorization") String accessToken) {
    authUseCase.logout(accessToken);

    return ResponseEntity.noContent().build();
  }
}
