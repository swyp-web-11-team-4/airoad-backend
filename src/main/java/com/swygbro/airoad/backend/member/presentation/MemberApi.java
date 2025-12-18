package com.swygbro.airoad.backend.member.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.swygbro.airoad.backend.auth.domain.dto.UserPrincipal;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.member.domain.dto.MemberNameResponse;
import com.swygbro.airoad.backend.member.domain.dto.MemberResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Member", description = "회원 정보 조회 API")
public interface MemberApi {

  @Operation(
      summary = "현재 로그인한 사용자 정보 조회",
      description =
          """
            JWT 토큰으로 인증된 사용자의 전체 정보를 반환합니다.
            """,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "현재 로그인한 사용자 정보 제공",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MemberResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "status": 200,
                              "data": {
                                "id": 1,
                                "name": "홍길동",
                                "email": "user@example.com",
                                "provider": "GOOGLE",
                                "profileImageUrl": "https://example.com/profile.jpg",
                                "createdAt": "2025-01-01T00:00:00",
                                "updatedAt": "2025-01-10T00:00:00"
                              }
                            }
                            """))),
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
                                "path": "/api/v1/members/me",
                                "errors": null
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "404",
        description = "회원을 찾을 수 없음",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "status": 404,
                              "data": {
                                "timestamp": "2025-12-19T10:00:00",
                                "code": "MEMBER001",
                                "message": "회원을 찾을 수 없습니다.",
                                "path": "/api/v1/members/me",
                                "errors": null
                              }
                            }
                            """)))
  })
  ResponseEntity<CommonResponse<MemberResponse>> getCurrentMember(
      @AuthenticationPrincipal UserPrincipal userPrincipal);

  @Operation(
      summary = "현재 로그인한 사용자 이름 조회",
      description =
          """
            JWT 토큰으로 인증된 사용자의 이름만 반환합니다.
            """,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "현재 로그인한 사용자 이름 제공",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MemberNameResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "status": 200,
                              "data": {
                                "name": "홍길동"
                              }
                            }
                            """))),
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
                                "path": "/api/v1/members/me/name",
                                "errors": null
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "404",
        description = "회원을 찾을 수 없음",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "status": 404,
                              "data": {
                                "timestamp": "2025-12-19T10:00:00",
                                "code": "MEMBER001",
                                "message": "회원을 찾을 수 없습니다.",
                                "path": "/api/v1/members/me/name",
                                "errors": null
                              }
                            }
                            """)))
  })
  ResponseEntity<CommonResponse<MemberNameResponse>> getCurrentMemberName(
      @AuthenticationPrincipal UserPrincipal userPrincipal);
}
