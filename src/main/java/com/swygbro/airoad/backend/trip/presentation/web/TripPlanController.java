package com.swygbro.airoad.backend.trip.presentation.web;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swygbro.airoad.backend.auth.domain.dto.UserPrincipal;
import com.swygbro.airoad.backend.common.domain.dto.CommonResponse;
import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;
import com.swygbro.airoad.backend.trip.application.DailyPlanCommandUseCase;
import com.swygbro.airoad.backend.trip.application.DailyPlanQueryUseCase;
import com.swygbro.airoad.backend.trip.application.TripPlanUseCase;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanUpdateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.ChannelIdResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanDetailResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanResponse;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Validated
public class TripPlanController implements TripPlanApi {

  private final TripPlanUseCase tripPlanUseCase;
  private final DailyPlanQueryUseCase dailyPlanQueryUseCase;
  private final DailyPlanCommandUseCase dailyPlanUseCase;

  @Override
  @GetMapping
  public ResponseEntity<CommonResponse<CursorPageResponse<TripPlanResponse>>> getUserTripPlans(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam(defaultValue = "10")
          @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
          @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
          int size,
      @RequestParam(required = false) @Positive(message = "커서 값은 1 이상이어야 합니다.") Long cursor,
      @RequestParam(required = false, defaultValue = "createdAt:desc")
          @Pattern(
              regexp = "^(createdAt|startDate):(asc|desc)$",
              message = "정렬 형식은 'field:direction' 형태여야 합니다.")
          String sort) {

    Long memberId = userPrincipal.getId();
    CursorPageResponse<TripPlanResponse> response =
        tripPlanUseCase.getUserTripPlans(memberId, size, cursor, sort);
    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, response));
  }

  @Override
  @GetMapping("/{tripPlanId}")
  public ResponseEntity<CommonResponse<TripPlanDetailResponse>> getTripPlanDetail(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable Long tripPlanId) {
    TripPlanDetailResponse detailResponse =
        tripPlanUseCase.getTripPlanDetail(tripPlanId, userPrincipal.getId());
    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, detailResponse));
  }

  @Override
  @PatchMapping("/{tripPlanId}")
  public ResponseEntity<Void> updateTripPlanTitle(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long tripPlanId,
      @Valid @RequestBody TripPlanUpdateRequest request) {
    tripPlanUseCase.updateTripPlan(tripPlanId, userPrincipal.getId(), request);
    return ResponseEntity.noContent().build();
  }

  @Override
  @DeleteMapping("/{tripPlanId}")
  public ResponseEntity<Void> deleteTripPlan(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable Long tripPlanId) {
    tripPlanUseCase.deleteTripPlan(tripPlanId, userPrincipal.getId());
    return ResponseEntity.noContent().build();
  }

  @PostMapping
  public ResponseEntity<CommonResponse<Object>> generateTripPlan(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Valid @RequestBody TripPlanCreateRequest request) {

    String username = userPrincipal.getUsername();
    ChannelIdResponse channelIdResponse = tripPlanUseCase.createTripPlanSession(username, request);

    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(CommonResponse.success(HttpStatus.ACCEPTED.value(), channelIdResponse));
  }

  @PostMapping("/{tripPlanId}")
  public ResponseEntity<CommonResponse<Object>> startTripPlanGeneration(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Parameter(description = "여행 계획 ID (tripPlanId)", example = "123", required = true)
          @PathVariable
          Long tripPlanId) {

    String username = userPrincipal.getUsername();
    tripPlanUseCase.startTripPlanGeneration(username, tripPlanId);

    return ResponseEntity.ok()
        .body(CommonResponse.success(HttpStatus.OK.value(), Map.of("message", "여행 일정 생성을 시작합니다.")));
  }

  @Override
  @GetMapping("/daily-plans/{tripPlanId}")
  public ResponseEntity<CommonResponse<List<DailyPlanResponse>>> getDailyPlans(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable Long tripPlanId) {

    List<DailyPlanResponse> dailyPlanResponseList =
        dailyPlanQueryUseCase.getDailyPlanListByTripPlanId(tripPlanId, userPrincipal.getId());

    return ResponseEntity.ok(CommonResponse.success(HttpStatus.OK, dailyPlanResponseList));
  }
}
