package com.swygbro.airoad.backend.member.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swygbro.airoad.backend.auth.domain.dto.UserPrincipal;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.member.application.MemberUseCase;
import com.swygbro.airoad.backend.member.domain.dto.MemberNameResponse;
import com.swygbro.airoad.backend.member.domain.dto.MemberResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController implements MemberApi {

  private final MemberUseCase memberUseCase;

  @Override
  @GetMapping(value = "/me")
  public ResponseEntity<CommonResponse<MemberResponse>> getCurrentMember(
      @AuthenticationPrincipal UserPrincipal userPrincipal) {

    String email = userPrincipal.getUsername();

    MemberResponse memberResponse = memberUseCase.getMemberByEmail(email);

    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, memberResponse));
  }

  @Override
  @GetMapping(value = "/me/name")
  public ResponseEntity<CommonResponse<MemberNameResponse>> getCurrentMemberName(
      @AuthenticationPrincipal UserPrincipal userPrincipal) {

    String email = userPrincipal.getUsername();
    MemberResponse memberResponse = memberUseCase.getMemberByEmail(email);
    MemberNameResponse response = new MemberNameResponse(memberResponse.name());

    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, response));
  }
}
