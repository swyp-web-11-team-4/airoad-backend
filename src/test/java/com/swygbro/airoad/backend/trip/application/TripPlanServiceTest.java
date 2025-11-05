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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.fixture.trip.TripPlanFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.exception.MemberErrorCode;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanResponse;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationRequestedEvent;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;
import com.swygbro.airoad.backend.trip.infrastructure.DailyPlanRepository;
import com.swygbro.airoad.backend.trip.infrastructure.ScheduledPlaceRepository;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;
import com.swygbro.airoad.backend.trip.infrastructure.TripThemeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("TripPlanService 테스트")
class TripPlanServiceTest {

  @Mock private TripPlanRepository tripPlanRepository;
  @Mock private DailyPlanRepository dailyPlanRepository;
  @Mock private ScheduledPlaceRepository scheduledPlaceRepository;
  @Mock private TripThemeRepository tripThemeRepository;
  @Mock private MemberRepository memberRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private TripPlanService tripPlanService;

  @Nested
  @DisplayName("사용자가 자신의 여행 일정 목록을 조회하면")
  class GetUserTripPlans {

    @Test
    @DisplayName("요청한 페이지에 맞는 여행 일정 목록이 반환된다")
    void returnsTripPlansForRequestedPage() {
      // given
      Long memberId = 1L;
      int size = 5;
      Member member = MemberFixture.create();
      List<TripPlan> tripPlans =
          List.of(
              TripPlanFixture.withId(2L, TripPlanFixture.createWithMember(member)),
              TripPlanFixture.withId(1L, TripPlanFixture.createWithMember(member)));
      Slice<TripPlan> tripPlanSlice = new SliceImpl<>(tripPlans, PageRequest.of(0, size), true);

      given(
              tripPlanRepository.findByMemberIdWithCursor(
                  eq(memberId), any(), any(PageRequest.class)))
          .willReturn(tripPlanSlice);

      // when
      CursorPageResponse<TripPlanResponse> response =
          tripPlanService.getUserTripPlans(memberId, size, null, "createdAt:desc");

      // then
      assertThat(response.getContent()).hasSize(tripPlans.size());
      assertThat(response.isHasNext()).isTrue();
      assertThat(response.getNextCursor()).isNotNull();
    }
  }

  @Nested
  @DisplayName("사용자가 여행 일정을 삭제하면")
  class DeleteTripPlan {

    @Test
    @DisplayName("자신의 여행 일정을 성공적으로 삭제한다")
    void deletesOwnTripPlanSuccessfully() {
      // given
      Long tripPlanId = 1L;
      Long memberId = 1L;
      given(tripPlanRepository.existsByIdAndMemberId(tripPlanId, memberId)).willReturn(true);
      willDoNothing().given(scheduledPlaceRepository).deleteByTripPlanId(tripPlanId);
      willDoNothing().given(dailyPlanRepository).deleteByTripPlanId(tripPlanId);
      willDoNothing().given(tripThemeRepository).deleteByTripPlanId(tripPlanId);
      willDoNothing().given(tripPlanRepository).deleteById(tripPlanId);

      // when
      tripPlanService.deleteTripPlan(tripPlanId, memberId);

      // then
      verify(tripPlanRepository).deleteById(tripPlanId);
    }

    @Test
    @DisplayName("존재하지 않는 여행 일정을 삭제하려 하면 예외가 발생한다")
    void throwsExceptionWhenDeletingNonExistentTripPlan() {
      // given
      Long tripPlanId = 99L;
      Long memberId = 1L;
      given(tripPlanRepository.existsByIdAndMemberId(tripPlanId, memberId)).willReturn(false);
      given(tripPlanRepository.existsById(tripPlanId)).willReturn(false);

      // when & then
      assertThatThrownBy(() -> tripPlanService.deleteTripPlan(tripPlanId, memberId))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", TripErrorCode.TRIP_PLAN_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사람의 여행 일정을 삭제하려 하면 예외가 발생한다")
    void throwsExceptionWhenDeletingOthersTripPlan() {
      // given
      Long tripPlanId = 2L;
      Long memberId = 1L;
      given(tripPlanRepository.existsByIdAndMemberId(tripPlanId, memberId)).willReturn(false);
      given(tripPlanRepository.existsById(tripPlanId)).willReturn(true);

      // when & then
      assertThatThrownBy(() -> tripPlanService.deleteTripPlan(tripPlanId, memberId))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", TripErrorCode.TRIP_PLAN_FORBIDDEN);
    }
  }

  @Nested
  @DisplayName("사용자가 새로운 여행 일정 생성을 요청하면")
  class RequestTripPlanGeneration {

    @Test
    @DisplayName("임시 여행 일정이 생성되고 생성 요청 이벤트가 발행된다")
    void createsTemporaryTripPlanAndPublishesEvent() {
      // given
      String username = "test@example.com";
      Long chatRoomId = 1L;
      TripPlanCreateRequest request =
          new TripPlanCreateRequest(List.of("맛집", "힐링"), LocalDate.now(), 3, "서울", 2);
      Member member = MemberFixture.createWithEmail(username);
      TripPlan savedTripPlan = TripPlanFixture.createWithMember(member);

      given(memberRepository.findByEmail(username)).willReturn(Optional.of(member));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(savedTripPlan);
      willDoNothing()
          .given(eventPublisher)
          .publishEvent(any(TripPlanGenerationRequestedEvent.class));

      // when
      tripPlanService.requestTripPlanGeneration(username, request, chatRoomId);

      // then
      ArgumentCaptor<TripPlan> tripPlanCaptor = ArgumentCaptor.forClass(TripPlan.class);
      verify(tripPlanRepository).save(tripPlanCaptor.capture());
      assertThat(tripPlanCaptor.getValue().getMember()).isEqualTo(member);
      assertThat(tripPlanCaptor.getValue().getTitle()).isEqualTo(request.region() + " 여행");

      ArgumentCaptor<TripPlanGenerationRequestedEvent> eventCaptor =
          ArgumentCaptor.forClass(TripPlanGenerationRequestedEvent.class);
      verify(eventPublisher).publishEvent(eventCaptor.capture());
      assertThat(eventCaptor.getValue().chatRoomId()).isEqualTo(chatRoomId);
      assertThat(eventCaptor.getValue().tripPlanId()).isEqualTo(savedTripPlan.getId());
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 요청하면 예외가 발생한다")
    void throwsExceptionForNonExistentUser() {
      // given
      String username = "nonexistent@example.com";
      Long chatRoomId = 1L;
      TripPlanCreateRequest request =
          new TripPlanCreateRequest(List.of("맛집"), LocalDate.now(), 3, "서울", 2);

      given(memberRepository.findByEmail(username)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(
              () -> tripPlanService.requestTripPlanGeneration(username, request, chatRoomId))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.MEMBER_NOT_FOUND);
    }
  }
}
