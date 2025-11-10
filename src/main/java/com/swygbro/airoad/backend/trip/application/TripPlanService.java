package com.swygbro.airoad.backend.trip.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.chat.application.AiConversationCommandUseCase;
import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.chat.exception.ChatErrorCode;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiConversationRepository;
import com.swygbro.airoad.backend.chat.infrastructure.repository.ConversationIdProjection;
import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.exception.MemberErrorCode;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanSortField;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanUpdateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.ChannelIdResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanDetailResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanResponse;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationRequestedEvent;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripPlanService implements TripPlanUseCase {

  // Trip 도메인 관련 레포지토리
  private final TripPlanRepository tripPlanRepository;

  // 회원 관련 레포지토리
  private final MemberRepository memberRepository;

  // 채팅 관련 레포지토리 및 유스케이스
  private final AiConversationRepository aiConversationRepository;
  private final AiConversationCommandUseCase aiConversationCommandUseCase;

  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional(readOnly = true)
  public CursorPageResponse<TripPlanResponse> getUserTripPlans(
      Long memberId, int size, Long cursor, String sort) {
    log.info("사용자 여행 일정 목록 조회 - memberId: {}, size: {}, cursor: {}", memberId, size, cursor);

    Sort sortBy = parseSort(sort);
    PageRequest pageRequest = PageRequest.of(0, size, sortBy);

    Specification<TripPlan> spec =
        (root, query, cb) -> cb.equal(root.get("member").get("id"), memberId);

    if (cursor != null) {
      TripPlan cursorPlan =
          tripPlanRepository
              .findByIdWithMember(cursor)
              .filter(tripPlan -> tripPlan.getMember().getId().equals(memberId))
              .orElseThrow(() -> new BusinessException(TripErrorCode.TRIP_PLAN_NOT_FOUND));

      Sort.Order order = sortBy.get().findFirst().orElse(Sort.Order.desc("createdAt"));
      TripPlanSortField sortField = TripPlanSortField.from(order.getProperty());

      spec = spec.and(sortField.getCursorSpecification(cursorPlan, order.getDirection()));
    }

    Page<TripPlan> tripPlansPage = tripPlanRepository.findAll(spec, pageRequest);
    List<TripPlan> content = tripPlansPage.getContent();
    boolean hasNext = tripPlansPage.hasNext();

    List<Long> tripPlanIds = content.stream().map(TripPlan::getId).toList();

    Map<Long, Long> conversationIdByTripPlanIdMap = Map.of();
    if (!tripPlanIds.isEmpty()) {
      List<ConversationIdProjection> projections =
          aiConversationRepository.findConversationIdsByTripPlanIds(tripPlanIds);
      conversationIdByTripPlanIdMap =
          projections.stream()
              .collect(
                  Collectors.toMap(
                      ConversationIdProjection::getTripPlanId,
                      ConversationIdProjection::getConversationId,
                      (existing, replacement) -> existing));
    }

    Long nextCursor =
        hasNext && !content.isEmpty() ? content.get(content.size() - 1).getId() : null;

    final Map<Long, Long> finalMap = conversationIdByTripPlanIdMap;
    List<TripPlanResponse> responses =
        content.stream()
            .map(tripPlan -> TripPlanResponse.of(tripPlan, finalMap.get(tripPlan.getId())))
            .toList();

    log.info(
        "사용자 여행 일정 목록 조회 완료 - memberId: {}, 조회된 일정 수: {}, hasNext: {}",
        memberId,
        responses.size(),
        hasNext);

    return CursorPageResponse.of(responses, nextCursor, hasNext);
  }

  @Override
  @Transactional(readOnly = true)
  public TripPlanDetailResponse getTripPlanDetail(Long tripPlanId, Long memberId) {
    log.info("여행 일정 상세 조회 - tripPlanId: {}, memberId: {}", tripPlanId, memberId);

    TripPlan tripPlan =
        tripPlanRepository
            .findByIdWithMember(tripPlanId)
            .orElseThrow(() -> new BusinessException(TripErrorCode.TRIP_PLAN_NOT_FOUND));

    if (!tripPlan.getMember().getId().equals(memberId)) {
      throw new BusinessException(TripErrorCode.TRIP_PLAN_FORBIDDEN);
    }

    return TripPlanDetailResponse.from(tripPlan);
  }

  @Override
  @Transactional
  public void deleteTripPlan(Long tripPlanId, Long memberId) {
    log.info("여행 일정 삭제 요청 - tripPlanId: {}, memberId: {}", tripPlanId, memberId);

    TripPlan tripPlan =
        tripPlanRepository
            .findByIdWithMember(tripPlanId)
            .orElseThrow(() -> new BusinessException(TripErrorCode.TRIP_PLAN_NOT_FOUND));

    if (!tripPlan.getMember().getId().equals(memberId)) {
      throw new BusinessException(TripErrorCode.TRIP_PLAN_FORBIDDEN);
    }

    aiConversationRepository
        .findByTripPlanId(tripPlanId)
        .ifPresent(
            conversation -> aiConversationCommandUseCase.deleteConversation(conversation.getId()));

    tripPlanRepository.delete(tripPlan);

    log.info("여행 일정 삭제 완료 - tripPlanId: {}, memberId: {}", tripPlanId, memberId);
  }

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

    // TripPlanTitle
    String title =
        request.region() + " " + request.duration() + "박 " + (request.duration() + 1) + "일 여행";
    // TripPlan 생성
    TripPlan tripPlan =
        TripPlan.builder()
            .member(member)
            .title(title)
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
  public void startTripPlanGeneration(String username, Long tripPlanId) {

    // 채팅방 조회
    AiConversation aiConversation =
        aiConversationRepository
            .findByTripPlanId(tripPlanId)
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

  @Override
  @Transactional
  public void updateTripPlan(Long tripPlanId, Long memberId, TripPlanUpdateRequest request) {
    log.info("여행 일정 수정 요청 - tripPlanId: {}, memberId: {}", tripPlanId, memberId);

    TripPlan tripPlan =
        tripPlanRepository
            .findByIdWithMember(tripPlanId)
            .orElseThrow(() -> new BusinessException(TripErrorCode.TRIP_PLAN_NOT_FOUND));

    if (!tripPlan.getMember().getId().equals(memberId)) {
      throw new BusinessException(TripErrorCode.TRIP_PLAN_FORBIDDEN);
    }

    applyUpdate(request.title(), tripPlan::updateTitle);

    log.info("여행 일정 수정 완료 - tripPlanId: {}", tripPlanId);
  }

  /**
   * 정렬 파라미터를 파싱하여 Sort 객체를 생성합니다.
   *
   * @param sort 정렬 파라미터 (형식: field:direction, 예: createdAt:desc)
   * @return Sort 객체
   */
  private Sort parseSort(String sort) {
    if (sort == null || sort.isBlank()) {
      sort = "createdAt:desc";
    }

    String[] parts = sort.split(":");
    String field = parts[0].trim();
    Sort.Direction direction =
        parts[1].trim().equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

    return Sort.by(direction, field).and(Sort.by(direction, "id"));
  }

  /**
   * 값이 null이 아닐 경우에만 Consumer의 업데이트 로직을 실행하는 헬퍼 메서드
   *
   * @param value 업데이트할 값
   * @param updater 업데이트 로직을 담은 Consumer
   * @param <T> 값의 타입
   */
  private <T> void applyUpdate(T value, Consumer<T> updater) {
    if (value != null) {
      updater.accept(value);
    }
  }
}
