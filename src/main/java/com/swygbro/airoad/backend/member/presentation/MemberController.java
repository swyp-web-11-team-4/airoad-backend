package com.swygbro.airoad.backend.member.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swygbro.airoad.backend.auth.domain.info.UserPrincipal;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.member.application.MemberUseCase;
import com.swygbro.airoad.backend.member.domain.dto.MemberResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/members") // ✅ 복수형으로 변경
@RequiredArgsConstructor
public class MemberController {

  private final MemberUseCase memberUseCase;

  @Operation(
      summary = "현재 로그인한 사용자 정보 조회",
      description = "JWT 토큰으로 인증된 사용자의 정보를 반환합니다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping(value = "/me") // ✅ GET으로 변경
  public ResponseEntity<CommonResponse<MemberResponse>> getCurrentMember(
      @AuthenticationPrincipal UserPrincipal userPrincipal) {

    String email = userPrincipal.getUsername();

    MemberResponse memberResponse = memberUseCase.getMemberByEmail(email);

    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, memberResponse));
  }
}
