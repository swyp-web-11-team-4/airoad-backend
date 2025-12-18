package com.swygbro.airoad.backend.auth.presentation.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.swygbro.airoad.backend.auth.domain.dto.request.ReissueTokenRequest;
import com.swygbro.airoad.backend.auth.domain.dto.response.TokenResponse;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth", description = "회원 인증 및 토큰 관리 API")
public interface AuthApi {

  @Operation(
      summary = "액세스 토큰 재발급",
      description = """
            리프레시 토큰을 사용하여 새로운 액세스 토큰과 리프레시 토큰을 발급받습니다.
            """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "토큰 재발급 성공",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TokenResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "status": 200,
                              "data": {
                                "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                "tokenType": "Bearer",
                                "expiresIn": 3600
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "status": 400,
                              "data": {
                                "timestamp": "2025-12-19T10:00:00",
                                "code": "COMMON002",
                                "message": "잘못된 요청입니다.",
                                "path": "/api/v1/auth/reissue",
                                "errors": null
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "유효하지 않은 토큰",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "만료된 리프레시 토큰",
                      value =
                          """
                          {
                            "success": false,
                            "status": 401,
                            "data": {
                              "timestamp": "2025-12-19T10:00:00",
                              "code": "AUTH003",
                              "message": "만료된 토큰입니다.",
                              "path": "/api/v1/auth/reissue",
                              "errors": null
                            }
                          }
                          """),
                  @ExampleObject(
                      name = "유효하지 않은 토큰",
                      value =
                          """
                          {
                            "success": false,
                            "status": 401,
                            "data": {
                              "timestamp": "2025-12-19T10:00:00",
                              "code": "AUTH002",
                              "message": "유효하지 않은 토큰입니다.",
                              "path": "/api/v1/auth/reissue",
                              "errors": null
                            }
                          }
                          """)
                }))
  })
  ResponseEntity<CommonResponse<TokenResponse>> reissueToken(
      @Parameter(
              description = "리프레시 토큰 (Bearer 접두사 포함)",
              required = true,
              example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
          @RequestBody
          ReissueTokenRequest request);

  @Operation(
      summary = "로그아웃",
      description = """
            현재 로그인된 사용자를 로그아웃 처리합니다.
            """,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
    @ApiResponse(
        responseCode = "401",
        description = "인증되지 않은 사용자",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "status": 401,
                              "data": {
                                "timestamp": "2025-12-19T10:00:00",
                                "code": "AUTH001",
                                "message": "인증이 필요합니다.",
                                "path": "/api/v1/auth/logout",
                                "errors": null
                              }
                            }
                            """)))
  })
  ResponseEntity<Void> logout(
      @Parameter(
              description = "액세스 토큰 (Bearer 접두사 포함)",
              required = true,
              example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
          @RequestHeader("Authorization")
          String accessToken);
}
