package com.swygbro.airoad.backend.trip.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.chat.application.AiConversationCommandUseCase;
import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiConversationRepository;
import com.swygbro.airoad.backend.chat.infrastructure.repository.ConversationIdProjection;
import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
import com.swygbro.airoad.backend.fixture.chat.AiConversationFixture;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.fixture.trip.TripPlanFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.exception.MemberErrorCode;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanUpdateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.ChannelIdResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanResponse;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationRequestedEvent;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("TripPlanService 테스트")
class TripPlanServiceTest {

  @Mock private TripPlanRepository tripPlanRepository;
  @Mock private MemberRepository memberRepository;
  @Mock private AiConversationRepository aiConversationRepository;
  @Mock private AiConversationCommandUseCase aiConversationCommandUseCase;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private TripPlanService tripPlanService;

  @Nested
  @DisplayName("사용자가 자신의 여행 일정 목록을 조회하면")
  class GetUserTripPlans {

    static class TestConversationIdProjection implements ConversationIdProjection {
      @Override
      public Long getTripPlanId() {
        return 2L;
      }

      @Override
      public Long getConversationId() {
        return 102L;
      }
    }

    @Test
    @DisplayName("요청한 페이지에 맞는 여행 일정 목록이 반환된다 (첫 페이지)")
    void returnsTripPlansForRequestedPageFirstPage() {
      // given
      Long memberId = 1L;
      int size = 5;
      Member member = MemberFixture.create();
      List<TripPlan> tripPlans =
          List.of(
              TripPlanFixture.withId(2L, TripPlanFixture.createWithMember(member)),
              TripPlanFixture.withId(1L, TripPlanFixture.createWithMember(member)));
      Page<TripPlan> tripPlanPage = new PageImpl<>(tripPlans, PageRequest.of(0, size), size + 1);

      given(tripPlanRepository.findAll(any(Specification.class), any(PageRequest.class)))
          .willReturn(tripPlanPage);

      given(aiConversationRepository.findConversationIdsByTripPlanIds(anyList()))
          .willReturn(List.of(new TestConversationIdProjection()));

      // when
      CursorPageResponse<TripPlanResponse> response =
          tripPlanService.getUserTripPlans(memberId, size, null, "createdAt:desc");

      // then
      assertThat(response.getContent()).hasSize(tripPlans.size());
      assertThat(response.isHasNext()).isTrue();
      assertThat(response.getNextCursor()).isNotNull();
    }

    @Test
    @DisplayName("커서가 주어진 경우 다음 페이지의 여행 일정 목록이 반환된다")
    void returnsTripPlansForRequestedPageWithCursor() {
      // given
      Long memberId = 1L;
      int size = 2;
      Long cursor = 3L;
      Member member = MemberFixture.withId(memberId, MemberFixture.create());

      TripPlan cursorTripPlan =
          TripPlanFixture.withId(cursor, TripPlanFixture.createWithMember(member));
      given(tripPlanRepository.findByIdWithMember(eq(cursor)))
          .willReturn(Optional.of(cursorTripPlan));

      List<TripPlan> nextTripPlans =
          List.of(
              TripPlanFixture.withId(2L, TripPlanFixture.createWithMember(member)),
              TripPlanFixture.withId(1L, TripPlanFixture.createWithMember(member)));
      Page<TripPlan> nextTripPlanPage =
          new PageImpl<>(nextTripPlans, PageRequest.of(0, size), nextTripPlans.size());

      given(tripPlanRepository.findAll(any(Specification.class), any(PageRequest.class)))
          .willReturn(nextTripPlanPage);

      given(aiConversationRepository.findConversationIdsByTripPlanIds(anyList()))
          .willReturn(List.of(new TestConversationIdProjection()));

      // when
      CursorPageResponse<TripPlanResponse> response =
          tripPlanService.getUserTripPlans(memberId, size, cursor, "createdAt:desc");

      // then
      assertThat(response.getContent()).hasSize(nextTripPlans.size());
      assertThat(response.isHasNext()).isFalse();
      assertThat(response.getNextCursor()).isNull();
    }
  }

  @Nested
  @DisplayName("사용자가 여행 일정을 삭제하면")
  class DeleteTripPlan {

    @Test
    @DisplayName("자신의 여행 일정을 성공적으로 삭제한다")
    void deletesOwnTripPlanSuccessfully() throws Exception {
      // given
      Long tripPlanId = 1L;
      Long memberId = 1L;
      Long conversationId = 10L;

      Member member = MemberFixture.withId(memberId, MemberFixture.create());
      TripPlan tripPlan = TripPlanFixture.createWithMember(member);
      AiConversation conversation =
          AiConversationFixture.createWithMemberAndTripPlan(member, tripPlan);
      var field = conversation.getClass().getSuperclass().getDeclaredField("id");
      field.setAccessible(true);
      field.set(conversation, conversationId);

      given(tripPlanRepository.findByIdWithMember(tripPlanId)).willReturn(Optional.of(tripPlan));
      given(aiConversationRepository.findByTripPlanId(tripPlanId))
          .willReturn(Optional.of(conversation));
      willDoNothing().given(aiConversationCommandUseCase).deleteConversation(conversationId);
      willDoNothing().given(tripPlanRepository).delete(tripPlan);

      // when
      tripPlanService.deleteTripPlan(tripPlanId, memberId);

      // then
      verify(tripPlanRepository).findByIdWithMember(tripPlanId);
      verify(aiConversationRepository).findByTripPlanId(tripPlanId);
      verify(aiConversationCommandUseCase).deleteConversation(conversationId);
      verify(tripPlanRepository).delete(tripPlan);
    }

    @Test
    @DisplayName("존재하지 않는 여행 일정을 삭제하려 하면 예외가 발생한다")
    void throwsExceptionWhenDeletingNonExistentTripPlan() {
      // given
      Long tripPlanId = 99L;
      Long memberId = 1L;
      given(tripPlanRepository.findByIdWithMember(tripPlanId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> tripPlanService.deleteTripPlan(tripPlanId, memberId))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", TripErrorCode.TRIP_PLAN_NOT_FOUND);

      verify(aiConversationCommandUseCase, never()).deleteConversation(anyLong());
      verify(tripPlanRepository, never()).delete(any(TripPlan.class));
    }

    @Test
    @DisplayName("다른 사람의 여행 일정을 삭제하려 하면 예외가 발생한다")
    void throwsExceptionWhenDeletingOthersTripPlan() {
      // given
      Long tripPlanId = 2L;
      Long memberId = 1L;
      Long otherMemberId = 2L;
      Member otherMember = MemberFixture.withId(otherMemberId, MemberFixture.create());
      TripPlan othersTripPlan = TripPlanFixture.createWithMember(otherMember);

      given(tripPlanRepository.findByIdWithMember(tripPlanId))
          .willReturn(Optional.of(othersTripPlan));

      // when & then
      assertThatThrownBy(() -> tripPlanService.deleteTripPlan(tripPlanId, memberId))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", TripErrorCode.TRIP_PLAN_FORBIDDEN);

      verify(aiConversationCommandUseCase, never()).deleteConversation(anyLong());
      verify(tripPlanRepository, never()).delete(any(TripPlan.class));
    }
  }

  @Nested
  @DisplayName("사용자가 여행 일정을 수정하면")
  class UpdateTripPlan {

    @Test
    @DisplayName("자신의 여행 일정을 성공적으로 수정한다")
    void updatesTripPlanSuccessfully() {
      // given
      Long tripPlanId = 1L;
      Long memberId = 1L;
      String newTitle = "변경된 여행 제목";
      TripPlanUpdateRequest request = new TripPlanUpdateRequest(newTitle);
      Member member = MemberFixture.withId(memberId, MemberFixture.create());
      TripPlan tripPlan = TripPlanFixture.createWithMember(member);

      given(tripPlanRepository.findByIdWithMember(tripPlanId)).willReturn(Optional.of(tripPlan));

      // when
      tripPlanService.updateTripPlan(tripPlanId, memberId, request);

      // then
      assertThat(tripPlan.getTitle()).isEqualTo(newTitle);
    }

    @Test
    @DisplayName("다른 사람의 여행 일정을 수정하려 하면 예외가 발생한다")
    void throwsExceptionWhenUpdatingOthersTripPlan() {
      // given
      Long tripPlanId = 1L;
      Long memberId = 1L;
      Long otherMemberId = 2L;
      TripPlanUpdateRequest request = new TripPlanUpdateRequest("변경된 여행 제목");
      Member otherMember = MemberFixture.withId(otherMemberId, MemberFixture.create());
      TripPlan tripPlan = TripPlanFixture.createWithMember(otherMember);

      given(tripPlanRepository.findByIdWithMember(tripPlanId)).willReturn(Optional.of(tripPlan));

      // when & then
      assertThatThrownBy(() -> tripPlanService.updateTripPlan(tripPlanId, memberId, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", TripErrorCode.TRIP_PLAN_FORBIDDEN);
    }

    @Test
    @DisplayName("존재하지 않는 여행 일정을 수정하려 하면 예외가 발생한다")
    void throwsExceptionWhenUpdatingNonExistentTripPlan() {
      // given
      Long tripPlanId = 99L;
      Long memberId = 1L;
      TripPlanUpdateRequest request = new TripPlanUpdateRequest("변경된 여행 제목");

      given(tripPlanRepository.findByIdWithMember(tripPlanId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> tripPlanService.updateTripPlan(tripPlanId, memberId, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", TripErrorCode.TRIP_PLAN_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("createTripPlanSession 메서드는")
  class CreateTripPlanSession {

    @Test
    @DisplayName("given 유효한 사용자와 요청 when 여행 계획 세션 생성 then 채팅방과 여행 계획이 생성되고 ID를 반환한다")
    void createTripPlanSessionSuccess() {
      // given
      String username = "test@example.com";
      Member member = MemberFixture.createWithEmail(username);
      LocalDate startDate = LocalDate.of(2025, 12, 1);
      Integer duration = 3;
      List<PlaceThemeType> themes = List.of(PlaceThemeType.HEALING, PlaceThemeType.RESTAURANT);

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(themes)
              .startDate(startDate)
              .duration(duration)
              .region("서울")
              .peopleCount(2)
              .build();

      TripPlan savedTripPlan = TripPlanFixture.createWithMember(member);
      AiConversation savedConversation =
          AiConversationFixture.createWithMemberAndTripPlan(member, savedTripPlan);

      given(memberRepository.findByEmail(username)).willReturn(Optional.of(member));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(savedTripPlan);
      given(aiConversationRepository.save(any(AiConversation.class))).willReturn(savedConversation);

      // when
      ChannelIdResponse response = tripPlanService.createTripPlanSession(username, request);

      // then
      assertThat(response).isNotNull();
      assertThat(response.conversationId()).isEqualTo(savedConversation.getId());
      assertThat(response.tripPlanId()).isEqualTo(savedTripPlan.getId());

      // TripPlan 저장 검증
      ArgumentCaptor<TripPlan> tripPlanCaptor = ArgumentCaptor.forClass(TripPlan.class);
      verify(tripPlanRepository).save(tripPlanCaptor.capture());
      TripPlan capturedTripPlan = tripPlanCaptor.getValue();

      assertThat(capturedTripPlan.getMember()).isEqualTo(member);
      assertThat(capturedTripPlan.getStartDate()).isEqualTo(startDate);
      assertThat(capturedTripPlan.getEndDate()).isEqualTo(startDate.plusDays(duration - 1));
      assertThat(capturedTripPlan.getRegion()).isEqualTo("서울");
      assertThat(capturedTripPlan.getPeopleCount()).isEqualTo(2);
      assertThat(capturedTripPlan.getTransportation()).isEqualTo(Transportation.PUBLIC_TRANSIT);
      assertThat(capturedTripPlan.getIsCompleted()).isFalse();
      assertThat(capturedTripPlan.getTitle()).isEqualTo("서울 3박 4일 여행");
      assertThat(capturedTripPlan.getTripThemes()).hasSize(themes.size());

      // AiConversation 저장 검증
      ArgumentCaptor<AiConversation> conversationCaptor =
          ArgumentCaptor.forClass(AiConversation.class);
      verify(aiConversationRepository).save(conversationCaptor.capture());
      AiConversation capturedConversation = conversationCaptor.getValue();

      assertThat(capturedConversation.getMember()).isEqualTo(member);
      assertThat(capturedConversation.getTripPlan()).isEqualTo(savedTripPlan);
    }

    @Test
    @DisplayName("given 존재하지 않는 사용자 when 여행 계획 세션 생성 then MEMBER_NOT_FOUND 예외가 발생한다")
    void createTripPlanSessionWithNonExistentMember() {
      // given
      String username = "nonexistent@example.com";
      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(List.of(PlaceThemeType.HEALING))
              .startDate(LocalDate.of(2025, 12, 1))
              .duration(3)
              .region("서울")
              .peopleCount(2)
              .build();

      given(memberRepository.findByEmail(username)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> tripPlanService.createTripPlanSession(username, request))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining(MemberErrorCode.MEMBER_NOT_FOUND.getDefaultMessage());

      verify(tripPlanRepository, times(0)).save(any(TripPlan.class));
      verify(aiConversationRepository, times(0)).save(any(AiConversation.class));
    }

    @Test
    @DisplayName("given 여러 테마 when 여행 계획 세션 생성 then 모든 테마가 추가된다")
    void createTripPlanSessionWithMultipleThemes() {
      // given
      String username = "test@example.com";
      Member member = MemberFixture.createWithEmail(username);
      List<PlaceThemeType> themes =
          List.of(
              PlaceThemeType.HEALING,
              PlaceThemeType.RESTAURANT,
              PlaceThemeType.EXPERIENCE_ACTIVITY);

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(themes)
              .startDate(LocalDate.of(2025, 12, 1))
              .duration(5)
              .region("제주")
              .peopleCount(4)
              .build();

      TripPlan savedTripPlan = TripPlanFixture.createWithMember(member);
      AiConversation savedConversation =
          AiConversationFixture.createWithMemberAndTripPlan(member, savedTripPlan);

      given(memberRepository.findByEmail(username)).willReturn(Optional.of(member));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(savedTripPlan);
      given(aiConversationRepository.save(any(AiConversation.class))).willReturn(savedConversation);

      // when
      tripPlanService.createTripPlanSession(username, request);

      // then
      ArgumentCaptor<TripPlan> tripPlanCaptor = ArgumentCaptor.forClass(TripPlan.class);
      verify(tripPlanRepository).save(tripPlanCaptor.capture());
      TripPlan capturedTripPlan = tripPlanCaptor.getValue();

      assertThat(capturedTripPlan.getTripThemes()).hasSize(3);
    }

    @Test
    @DisplayName("given 1일 여행 when 여행 계획 세션 생성 then 시작일과 종료일이 같다")
    void createTripPlanSessionWithOneDayTrip() {
      // given
      String username = "test@example.com";
      Member member = MemberFixture.createWithEmail(username);
      LocalDate startDate = LocalDate.of(2025, 12, 1);

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(List.of(PlaceThemeType.HEALING))
              .startDate(startDate)
              .duration(1)
              .region("서울")
              .peopleCount(1)
              .build();

      TripPlan savedTripPlan = TripPlanFixture.createWithMember(member);
      AiConversation savedConversation =
          AiConversationFixture.createWithMemberAndTripPlan(member, savedTripPlan);

      given(memberRepository.findByEmail(username)).willReturn(Optional.of(member));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(savedTripPlan);
      given(aiConversationRepository.save(any(AiConversation.class))).willReturn(savedConversation);

      // when
      tripPlanService.createTripPlanSession(username, request);

      // then
      ArgumentCaptor<TripPlan> tripPlanCaptor = ArgumentCaptor.forClass(TripPlan.class);
      verify(tripPlanRepository).save(tripPlanCaptor.capture());
      TripPlan capturedTripPlan = tripPlanCaptor.getValue();

      assertThat(capturedTripPlan.getStartDate()).isEqualTo(startDate);
      assertThat(capturedTripPlan.getEndDate()).isEqualTo(startDate);
    }

    @Test
    @DisplayName("given 여행 계획 요청 when 세션 생성 then 제목이 '지역명 X박 Y일 여행' 형식으로 생성된다")
    void createTripPlanSessionWithAutoGeneratedTitle() {
      // given
      String username = "test@example.com";
      Member member = MemberFixture.createWithEmail(username);

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(List.of(PlaceThemeType.HEALING))
              .startDate(LocalDate.of(2025, 12, 1))
              .duration(5)
              .region("제주")
              .peopleCount(2)
              .build();

      TripPlan savedTripPlan = TripPlanFixture.createWithMember(member);
      AiConversation savedConversation =
          AiConversationFixture.createWithMemberAndTripPlan(member, savedTripPlan);

      given(memberRepository.findByEmail(username)).willReturn(Optional.of(member));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(savedTripPlan);
      given(aiConversationRepository.save(any(AiConversation.class))).willReturn(savedConversation);

      // when
      tripPlanService.createTripPlanSession(username, request);

      // then
      ArgumentCaptor<TripPlan> tripPlanCaptor = ArgumentCaptor.forClass(TripPlan.class);
      verify(tripPlanRepository).save(tripPlanCaptor.capture());
      TripPlan capturedTripPlan = tripPlanCaptor.getValue();

      assertThat(capturedTripPlan.getTitle()).isEqualTo("제주 5박 6일 여행");
    }

    @Test
    @DisplayName("given 1박 여행 요청 when 세션 생성 then 제목이 '지역명 1박 2일 여행'으로 생성된다")
    void createTripPlanSessionWithOneDayTripTitle() {
      // given
      String username = "test@example.com";
      Member member = MemberFixture.createWithEmail(username);

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(List.of(PlaceThemeType.HEALING))
              .startDate(LocalDate.of(2025, 12, 1))
              .duration(1)
              .region("부산")
              .peopleCount(1)
              .build();

      TripPlan savedTripPlan = TripPlanFixture.createWithMember(member);
      AiConversation savedConversation =
          AiConversationFixture.createWithMemberAndTripPlan(member, savedTripPlan);

      given(memberRepository.findByEmail(username)).willReturn(Optional.of(member));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(savedTripPlan);
      given(aiConversationRepository.save(any(AiConversation.class))).willReturn(savedConversation);

      // when
      tripPlanService.createTripPlanSession(username, request);

      // then
      ArgumentCaptor<TripPlan> tripPlanCaptor = ArgumentCaptor.forClass(TripPlan.class);
      verify(tripPlanRepository).save(tripPlanCaptor.capture());
      TripPlan capturedTripPlan = tripPlanCaptor.getValue();

      assertThat(capturedTripPlan.getTitle()).isEqualTo("부산 1박 2일 여행");
    }

    @Test
    @DisplayName("given 다양한 지역명 when 세션 생성 then 해당 지역명이 제목에 포함된다")
    void createTripPlanSessionWithVariousRegionNames() {
      // given
      String username = "test@example.com";
      Member member = MemberFixture.createWithEmail(username);

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(List.of(PlaceThemeType.HEALING))
              .startDate(LocalDate.of(2025, 12, 1))
              .duration(2)
              .region("강릉")
              .peopleCount(3)
              .build();

      TripPlan savedTripPlan = TripPlanFixture.createWithMember(member);
      AiConversation savedConversation =
          AiConversationFixture.createWithMemberAndTripPlan(member, savedTripPlan);

      given(memberRepository.findByEmail(username)).willReturn(Optional.of(member));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(savedTripPlan);
      given(aiConversationRepository.save(any(AiConversation.class))).willReturn(savedConversation);

      // when
      tripPlanService.createTripPlanSession(username, request);

      // then
      ArgumentCaptor<TripPlan> tripPlanCaptor = ArgumentCaptor.forClass(TripPlan.class);
      verify(tripPlanRepository).save(tripPlanCaptor.capture());
      TripPlan capturedTripPlan = tripPlanCaptor.getValue();

      assertThat(capturedTripPlan.getTitle()).isEqualTo("강릉 2박 3일 여행");
    }
  }

  @Nested
  @DisplayName("startTripPlanGeneration 메서드는")
  class StartTripPlanGeneration {

    @Test
    @DisplayName("given 유효한 채팅방 ID when 여행 계획 생성 시작 then 이벤트가 발행된다")
    void startTripPlanGenerationSuccess() throws Exception {
      // given
      String username = "test@example.com";
      Long chatRoomId = 1L;
      Long tripPlanId = 50L;

      Member member = MemberFixture.createWithEmail(username);
      TripPlan tripPlan = TripPlanFixture.createWithMember(member);

      // Reflection을 사용하여 TripPlan ID 설정
      var tripPlanIdField = tripPlan.getClass().getSuperclass().getDeclaredField("id");
      tripPlanIdField.setAccessible(true);
      tripPlanIdField.set(tripPlan, tripPlanId);

      AiConversation aiConversation =
          AiConversationFixture.createWithMemberAndTripPlan(member, tripPlan);

      // Reflection을 사용하여 AiConversation ID 설정
      var conversationIdField = aiConversation.getClass().getSuperclass().getDeclaredField("id");
      conversationIdField.setAccessible(true);
      conversationIdField.set(aiConversation, chatRoomId);

      given(aiConversationRepository.findByTripPlanId(tripPlanId))
          .willReturn(Optional.of(aiConversation));

      // when
      tripPlanService.startTripPlanGeneration(username, tripPlanId);

      // then
      ArgumentCaptor<TripPlanGenerationRequestedEvent> eventCaptor =
          ArgumentCaptor.forClass(TripPlanGenerationRequestedEvent.class);
      verify(eventPublisher).publishEvent(eventCaptor.capture());
      TripPlanGenerationRequestedEvent capturedEvent = eventCaptor.getValue();

      assertThat(capturedEvent.chatRoomId()).isEqualTo(chatRoomId);
      assertThat(capturedEvent.tripPlanId()).isEqualTo(tripPlanId);
      assertThat(capturedEvent.username()).isEqualTo(username);
    }

    @Test
    @DisplayName("given 존재하지 않는 채팅방 when 여행 계획 생성 시작 then 예외가 발생한다")
    void startTripPlanGenerationWithNonExistentChatRoom() {
      // given
      String username = "test@example.com";
      Long tripPlanId = 999L;

      given(aiConversationRepository.findByTripPlanId(tripPlanId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> tripPlanService.startTripPlanGeneration(username, tripPlanId))
          .isInstanceOf(Exception.class);

      verify(eventPublisher, times(0)).publishEvent(any(TripPlanGenerationRequestedEvent.class));
    }

    @Test
    @DisplayName("given 여행 계획이 연결된 채팅방 when 여행 계획 생성 시작 then 올바른 여행 계획 ID로 이벤트가 발행된다")
    void startTripPlanGenerationWithValidTripPlan() throws Exception {
      // given
      String username = "test@example.com";
      Long tripPlanId = 100L;
      Long chatRoomId = 1L;
      Member member = MemberFixture.createWithEmail(username);
      TripPlan tripPlan = TripPlanFixture.createWithMember(member);

      // Reflection을 사용하여 TripPlan ID 설정
      var tripPlanIdField = tripPlan.getClass().getSuperclass().getDeclaredField("id");
      tripPlanIdField.setAccessible(true);
      tripPlanIdField.set(tripPlan, tripPlanId);

      AiConversation aiConversation =
          AiConversationFixture.createWithMemberAndTripPlan(member, tripPlan);

      // Reflection을 사용하여 AiConversation ID 설정
      var conversationIdField = aiConversation.getClass().getSuperclass().getDeclaredField("id");
      conversationIdField.setAccessible(true);
      conversationIdField.set(aiConversation, chatRoomId);

      given(aiConversationRepository.findByTripPlanId(tripPlanId))
          .willReturn(Optional.of(aiConversation));

      // when
      tripPlanService.startTripPlanGeneration(username, tripPlanId);

      // then
      ArgumentCaptor<TripPlanGenerationRequestedEvent> eventCaptor =
          ArgumentCaptor.forClass(TripPlanGenerationRequestedEvent.class);
      verify(eventPublisher).publishEvent(eventCaptor.capture());
      TripPlanGenerationRequestedEvent capturedEvent = eventCaptor.getValue();

      assertThat(capturedEvent.tripPlanId()).isEqualTo(tripPlanId);
      assertThat(capturedEvent.chatRoomId()).isEqualTo(chatRoomId);
    }
  }
}
