package com.swygbro.airoad.backend.auth.presentation.web;

import com.swygbro.airoad.backend.auth.application.AuthUseCase;
import com.swygbro.airoad.backend.auth.domain.dto.response.TokenResponse;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "v1-auth", description = "회원 인증을 담당하는 API")
public class AuthController {

  private static final String BEARER_PREFIX = "Bearer ";

  private final AuthUseCase authUseCase;

  @PostMapping("/reissue")
  public ResponseEntity<CommonResponse<TokenResponse>> reissueToken(
      @RequestHeader("Authorization") String refreshToken) {
    TokenResponse tokenResponse = authUseCase.reissue(extractToken(refreshToken));

    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, tokenResponse));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestHeader("Authorization") String accessToken) {
    authUseCase.logout(extractToken(accessToken));

    return ResponseEntity.noContent().build();
  }

  private String extractToken (String token){
    if (token != null && token.startsWith(BEARER_PREFIX)) {
      return token.substring(BEARER_PREFIX.length());
    }
    return token;
  }
}
