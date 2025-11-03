package com.swygbro.airoad.backend.trip.application;

import java.time.LocalDate;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.exception.MemberErrorCode;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationRequestedEvent;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import lombok.RequiredArgsConstructor;

/** 여행 계획 관련 비즈니스 로직을 처리하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class TripService implements TripUseCase {

  private final ApplicationEventPublisher eventPublisher;
  private final MemberRepository memberRepository;
  private final TripPlanRepository tripPlanRepository;

  /**
   * 여행 일정 생성을 요청합니다.
   *
   * <p>사용자가 입력한 여행 조건을 바탕으로 임시 TripPlan을 생성하고, AI 기반 여행 일정 생성 이벤트를 발행합니다.
   *
   * @param username 사용자 이름 (이메일)
   * @param request 여행 일정 생성 요청 정보
   * @param chatRoomId 채팅방 ID
   */
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
            .transportation(Transportation.NONE)
            .peopleCount(request.peopleCount())
            .build();

    TripPlan savedTripPlan = tripPlanRepository.save(tripPlan);

    // TripPlanCreateRequest 변환
    TripPlanCreateRequest tripPlanCreateRequest =
        TripPlanCreateRequest.builder()
            .themes(request.themes())
            .startDate(request.startDate())
            .duration(request.duration())
            .region(request.region())
            .peopleCount(request.peopleCount())
            .build();

    // 이벤트 발행
    TripPlanGenerationRequestedEvent event =
        TripPlanGenerationRequestedEvent.builder()
            .chatRoomId(chatRoomId)
            .tripPlanId(savedTripPlan.getId())
            .username(username)
            .request(tripPlanCreateRequest)
            .build();

    eventPublisher.publishEvent(event);
  }
}
