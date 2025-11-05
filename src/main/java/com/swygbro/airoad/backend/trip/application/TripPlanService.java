package com.swygbro.airoad.backend.trip.application;

import java.time.LocalDate;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.exception.MemberErrorCode;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanResponse;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationRequestedEvent;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;
import com.swygbro.airoad.backend.trip.infrastructure.DailyPlanRepository;
import com.swygbro.airoad.backend.trip.infrastructure.ScheduledPlaceRepository;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;
import com.swygbro.airoad.backend.trip.infrastructure.TripThemeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripPlanService implements TripPlanUseCase {

  private final TripPlanRepository tripPlanRepository;
  private final DailyPlanRepository dailyPlanRepository;
  private final ScheduledPlaceRepository scheduledPlaceRepository;
  private final TripThemeRepository tripThemeRepository;
  private final MemberRepository memberRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  public CursorPageResponse<TripPlanResponse> getUserTripPlans(
      Long memberId, int size, Long cursor, String sort) {
    log.info("사용자 여행 일정 목록 조회 - memberId: {}, size: {}, cursor: {}", memberId, size, cursor);

    // 정렬 파라미터 파싱
    Sort sortBy = parseSort(sort);
    PageRequest pageRequest = PageRequest.of(0, size, sortBy);

    // Slice를 사용하여 데이터 조회
    Slice<TripPlan> tripPlans =
        tripPlanRepository.findByMemberIdWithCursor(memberId, cursor, pageRequest);

    // hasNext 판단 및 실제 컨텐츠 추출
    List<TripPlan> content = tripPlans.getContent();
    boolean hasNext = tripPlans.hasNext();

    // nextCursor 설정
    Long nextCursor =
        hasNext && !content.isEmpty() ? content.get(content.size() - 1).getId() : null;

    // 응답 변환
    List<TripPlanResponse> responses = content.stream().map(TripPlanResponse::of).toList();

    log.info(
        "사용자 여행 일정 목록 조회 완료 - memberId: {}, 조회된 일정 수: {}, hasNext: {}",
        memberId,
        responses.size(),
        hasNext);

    return CursorPageResponse.of(responses, nextCursor, hasNext);
  }

  @Override
  @Transactional
  public void deleteTripPlan(Long tripPlanId, Long memberId) {
    log.info("여행 일정 삭제 요청 - tripPlanId: {}, memberId: {}", tripPlanId, memberId);

    if (!tripPlanRepository.existsByIdAndMemberId(tripPlanId, memberId)) {
      if (!tripPlanRepository.existsById(tripPlanId)) {
        throw new BusinessException(TripErrorCode.TRIP_PLAN_NOT_FOUND);
      } else {
        throw new BusinessException(TripErrorCode.TRIP_PLAN_FORBIDDEN);
      }
    }

    scheduledPlaceRepository.deleteByTripPlanId(tripPlanId);
    dailyPlanRepository.deleteByTripPlanId(tripPlanId);
    tripThemeRepository.deleteByTripPlanId(tripPlanId);
    tripPlanRepository.deleteById(tripPlanId);

    log.info("여행 일정 삭제 완료 - tripPlanId: {}, memberId: {}", tripPlanId, memberId);
  }

  @Override
  @Transactional
  public void requestTripPlanGeneration(
      String username, TripPlanCreateRequest request, Long chatRoomId) {

    // 사용자 조회
    Member member =
        memberRepository
            .findByEmail(username)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

    // 여행 종료일 계산 (시작일 + 기간 - 1일)
    LocalDate endDate = request.startDate().plusDays(request.duration() - 1);

    // 임시 TripPlan 생성 (AI 일정 생성 전)
    TripPlan tripPlan =
        TripPlan.builder()
            .member(member)
            .title(request.region() + " 여행")
            .startDate(request.startDate())
            .endDate(endDate)
            .isCompleted(false)
            .region(request.region())
            .transportation(Transportation.PUBLIC_TRANSIT)
            .peopleCount(request.peopleCount())
            .build();

    TripPlan savedTripPlan = tripPlanRepository.save(tripPlan);

    // 이벤트 발행
    TripPlanGenerationRequestedEvent event =
        TripPlanGenerationRequestedEvent.builder()
            .chatRoomId(chatRoomId)
            .tripPlanId(savedTripPlan.getId())
            .username(username)
            .request(request)
            .build();

    eventPublisher.publishEvent(event);
  }

  /**
   * 정렬 파라미터를 파싱하여 Sort 객체를 생성합니다.
   *
   * @param sort 정렬 파라미터 (형식: field:direction, 예: createdAt:desc)
   * @return Sort 객체
   */
  private Sort parseSort(String sort) {
    if (sort == null || sort.isBlank()) {
      return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    String[] parts = sort.split(":");
    String field = parts[0].trim();
    String direction = parts[1].trim().toLowerCase();

    // Sort 객체 생성
    Sort.Direction sortDirection =
        direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    return Sort.by(sortDirection, field);
  }
}
