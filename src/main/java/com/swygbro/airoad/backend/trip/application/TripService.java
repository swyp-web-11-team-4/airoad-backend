package com.swygbro.airoad.backend.trip.application;

import java.time.LocalDate;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.chat.exception.ChatErrorCode;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiConversationRepository;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.exception.MemberErrorCode;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.ChannelIdResponse;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationRequestedEvent;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import lombok.RequiredArgsConstructor;

/** 여행 계획 관련 비즈니스 로직을 처리하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class TripService implements TripUseCase {

  private final ApplicationEventPublisher eventPublisher;
  private final MemberRepository memberRepository;
  private final TripPlanRepository tripPlanRepository;
  private final AiConversationRepository aiConversationRepository;

  /**
   * 여행 일정 생성 세션을 시작합니다.
   *
   * <p>사용자가 사용할 채팅방과 여행계획을 생성하고 그 id를 반환합니다.
   *
   * @param username 사용자 이름 (이메일)
   * @param request 여행 계획 요청 dto
   * @return 채팅방 ID와 여행 계획 ID
   */
  @Override
  @Transactional
  public ChannelIdResponse createTripPlanSession(String username, TripPlanCreateRequest request) {

    // 사용자 조회
    Member member =
        memberRepository
            .findByEmail(username)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

    // 여행 종료일 계산
    LocalDate endDate = request.startDate().plusDays(request.duration() - 1);

    // TripPlan 생성
    TripPlan tripPlan =
        TripPlan.builder()
            .member(member)
            .title("") // 초기 제목은 비어있음
            .startDate(request.startDate())
            .endDate(endDate)
            .isCompleted(false)
            .region(request.region())
            .transportation(Transportation.PUBLIC_TRANSIT)
            .peopleCount(request.peopleCount())
            .build();

    // 테마 추가
    for (PlaceThemeType placeThemeType : request.themes()) {
      tripPlan.addTripTheme(placeThemeType);
    }

    TripPlan savedTripPlan = tripPlanRepository.save(tripPlan);

    // 채팅방 생성
    AiConversation aiConversation =
        AiConversation.builder().member(member).tripPlan(savedTripPlan).build();
    aiConversationRepository.save(aiConversation);

    return new ChannelIdResponse(aiConversation.getId(), savedTripPlan.getId());
  }

  // 일정 생성을 위한 이벤트를 배포합니다
  @Override
  @Transactional
  public void startTripPlanGeneration(String username, Long chatRoomId) {

    // 채팅방 조회
    AiConversation aiConversation =
        aiConversationRepository
            .findById(chatRoomId)
            .orElseThrow(() -> new BusinessException(ChatErrorCode.CONVERSATION_NOT_FOUND));

    // 권한 검증: 채팅방 소유자와 요청자가 일치하는지 확인
    if (!aiConversation.getMember().getEmail().equals(username)) {
      throw new BusinessException(ChatErrorCode.CONVERSATION_ACCESS_DENIED);
    }

    // 여행 계획 존재 여부 확인
    TripPlan tripPlan = aiConversation.getTripPlan();
    if (tripPlan == null) {
      throw new BusinessException(TripErrorCode.TRIP_PLAN_NOT_FOUND);
    }

    // TripPlan으로부터 TripPlanCreateRequest 재구성
    int duration =
        (int) (tripPlan.getEndDate().toEpochDay() - tripPlan.getStartDate().toEpochDay() + 1);
    TripPlanCreateRequest request =
        TripPlanCreateRequest.builder()
            .themes(tripPlan.getTripThemes())
            .startDate(tripPlan.getStartDate())
            .duration(duration)
            .region(tripPlan.getRegion())
            .peopleCount(tripPlan.getPeopleCount())
            .build();

    // 이벤트 발행 (TripPlan은 이미 생성되어 있음)
    TripPlanGenerationRequestedEvent event =
        TripPlanGenerationRequestedEvent.builder()
            .chatRoomId(aiConversation.getId())
            .tripPlanId(tripPlan.getId())
            .username(username)
            .request(request)
            .build();

    eventPublisher.publishEvent(event);
  }
}
