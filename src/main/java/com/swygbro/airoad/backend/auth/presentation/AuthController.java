package com.swygbro.airoad.backend.auth.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swygbro.airoad.backend.auth.application.AuthUseCase;
import com.swygbro.airoad.backend.auth.domain.dto.LoginRequest;
import com.swygbro.airoad.backend.auth.domain.dto.LoginResponse;
import com.swygbro.airoad.backend.auth.domain.dto.TokenResponse;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

  @Operation(
      summary = "소셜 로그인",
      description = "제공처의 액세스 토큰으로 회원 정보 조회, 로그인 처리 후 액세스 토큰과 회원 정보를 응답으로 전송")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "로그인 성공",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponse.class)))
      })
  @PostMapping(value = "/login")
  public ResponseEntity<CommonResponse<LoginResponse>> login(@RequestBody LoginRequest request) {

    LoginResponse loginResponse =
        authUseCase.socialLogin(request.getProvider(), request.getProviderAccessToken());

    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, loginResponse));
  }

  @Operation(summary = "액세스 토큰 재발급", description = "리프레시 토큰이 유효하다면, 새로운 액세스 토큰 발급")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "액세스 토큰 재발급 성공",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TokenResponse.class)))
      })
  @PostMapping(value = "/reissue-token")
  public ResponseEntity<CommonResponse<TokenResponse>> reissueToken(
      @RequestBody String refreshToken) {

    TokenResponse tokenResponse = authUseCase.reissueToken(refreshToken);

    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, tokenResponse));
  }

  @Operation(summary = "로그아웃", description = "Refresh Token을 삭제하여 로그아웃 처리")
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestBody String refreshToken) {

    authUseCase.logout(refreshToken);

    return ResponseEntity.ok().build();
  }
}
