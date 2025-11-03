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
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;
import com.swygbro.airoad.backend.member.exception.MemberErrorCode;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationRequestedEvent;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class TripServiceTest {

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private MemberRepository memberRepository;

  @Mock private TripPlanRepository tripPlanRepository;

  @InjectMocks private TripService tripService;

  @Nested
  @DisplayName("requestTripPlanGeneration 메서드는")
  class RequestTripPlanGeneration {

    @Test
    @DisplayName("유효한 요청으로 TripPlan을 생성하고 이벤트를 발행한다")
    void shouldCreateTripPlanAndPublishEvent() {
      // given
      String username = "test@naver.com";
      Long chatRoomId = 1L;

      Member member =
          Member.builder()
              .email(username)
              .name("테스트")
              .imageUrl("https://example.com/image.jpg")
              .provider(ProviderType.GOOGLE)
              .role(MemberRole.MEMBER)
              .build();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .region("제주")
              .startDate(LocalDate.of(2025, 3, 1))
              .duration(3)
              .themes(List.of("힐링", "맛집"))
              .peopleCount(2)
              .build();

      TripPlan savedTripPlan =
          TripPlan.builder()
              .member(member)
              .title("제주 여행")
              .startDate(LocalDate.of(2025, 3, 1))
              .endDate(LocalDate.of(2025, 3, 3))
              .isCompleted(false)
              .region("제주")
              .peopleCount(2)
              .build();

      given(memberRepository.findByEmail(username)).willReturn(Optional.of(member));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(savedTripPlan);

      // when
      tripService.requestTripPlanGeneration(username, request, chatRoomId);

      // then
      // TripPlan 저장 검증
      ArgumentCaptor<TripPlan> tripPlanCaptor = ArgumentCaptor.forClass(TripPlan.class);
      verify(tripPlanRepository, times(1)).save(tripPlanCaptor.capture());

      TripPlan capturedTripPlan = tripPlanCaptor.getValue();
      assertThat(capturedTripPlan.getMember()).isEqualTo(member);
      assertThat(capturedTripPlan.getTitle()).isEqualTo("제주 여행");
      assertThat(capturedTripPlan.getStartDate()).isEqualTo(LocalDate.of(2025, 3, 1));
      assertThat(capturedTripPlan.getEndDate()).isEqualTo(LocalDate.of(2025, 3, 3));
      assertThat(capturedTripPlan.getIsCompleted()).isFalse();
      assertThat(capturedTripPlan.getRegion()).isEqualTo("제주");
      assertThat(capturedTripPlan.getPeopleCount()).isEqualTo(2);

      // 이벤트 발행 검증
      ArgumentCaptor<TripPlanGenerationRequestedEvent> eventCaptor =
          ArgumentCaptor.forClass(TripPlanGenerationRequestedEvent.class);
      verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

      TripPlanGenerationRequestedEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.chatRoomId()).isEqualTo(chatRoomId);
      assertThat(capturedEvent.tripPlanId()).isEqualTo(savedTripPlan.getId());
      assertThat(capturedEvent.username()).isEqualTo(username);
      assertThat(capturedEvent.request().themes()).isEqualTo(request.themes());
      assertThat(capturedEvent.request().startDate()).isEqualTo(request.startDate());
      assertThat(capturedEvent.request().duration()).isEqualTo(request.duration());
      assertThat(capturedEvent.request().region()).isEqualTo(request.region());
      assertThat(capturedEvent.request().peopleCount()).isEqualTo(request.peopleCount());
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 요청하면 MEMBER_NOT_FOUND 예외를 발생시킨다")
    void shouldThrowExceptionWhenMemberNotFound() {
      // given
      String username = "nonexistent@naver.com";
      Long chatRoomId = 1L;

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .region("제주")
              .startDate(LocalDate.of(2025, 3, 1))
              .duration(3)
              .themes(List.of("힐링", "맛집"))
              .peopleCount(2)
              .build();

      given(memberRepository.findByEmail(username)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> tripService.requestTripPlanGeneration(username, request, chatRoomId))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining(MemberErrorCode.MEMBER_NOT_FOUND.getDefaultMessage());

      // TripPlan이 저장되지 않았는지 검증
      verify(tripPlanRepository, times(0)).save(any(TripPlan.class));
      // 이벤트가 발행되지 않았는지 검증
      verify(eventPublisher, times(0)).publishEvent(any(TripPlanGenerationRequestedEvent.class));
    }

    @Test
    @DisplayName("여행 종료일을 올바르게 계산한다 (시작일 + 기간 - 1일)")
    void shouldCalculateEndDateCorrectly() {
      // given
      String username = "test@naver.com";
      Long chatRoomId = 1L;

      Member member =
          Member.builder()
              .email(username)
              .name("테스트")
              .imageUrl("https://example.com/image.jpg")
              .provider(ProviderType.GOOGLE)
              .role(MemberRole.MEMBER)
              .build();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .region("부산")
              .startDate(LocalDate.of(2025, 4, 10))
              .duration(5) // 4박 5일
              .themes(List.of("액티비티"))
              .peopleCount(4)
              .build();

      TripPlan savedTripPlan =
          TripPlan.builder()
              .member(member)
              .title("부산 여행")
              .startDate(LocalDate.of(2025, 4, 10))
              .endDate(LocalDate.of(2025, 4, 14)) // 4월 10일 + 5일 - 1일 = 4월 14일
              .isCompleted(false)
              .region("부산")
              .peopleCount(4)
              .build();

      given(memberRepository.findByEmail(username)).willReturn(Optional.of(member));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(savedTripPlan);

      // when
      tripService.requestTripPlanGeneration(username, request, chatRoomId);

      // then
      ArgumentCaptor<TripPlan> tripPlanCaptor = ArgumentCaptor.forClass(TripPlan.class);
      verify(tripPlanRepository, times(1)).save(tripPlanCaptor.capture());

      TripPlan capturedTripPlan = tripPlanCaptor.getValue();
      assertThat(capturedTripPlan.getStartDate()).isEqualTo(LocalDate.of(2025, 4, 10));
      assertThat(capturedTripPlan.getEndDate()).isEqualTo(LocalDate.of(2025, 4, 14));
    }

    @Test
    @DisplayName("여행 제목을 지역명 + '여행' 형태로 생성한다")
    void shouldGenerateTitleWithRegionName() {
      // given
      String username = "test@naver.com";
      Long chatRoomId = 1L;

      Member member =
          Member.builder()
              .email(username)
              .name("테스트")
              .imageUrl("https://example.com/image.jpg")
              .provider(ProviderType.GOOGLE)
              .role(MemberRole.MEMBER)
              .build();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .region("강릉")
              .startDate(LocalDate.of(2025, 5, 1))
              .duration(2)
              .themes(List.of("바다"))
              .peopleCount(1)
              .build();

      TripPlan savedTripPlan =
          TripPlan.builder()
              .member(member)
              .title("강릉 여행")
              .startDate(LocalDate.of(2025, 5, 1))
              .endDate(LocalDate.of(2025, 5, 2))
              .isCompleted(false)
              .region("강릉")
              .peopleCount(1)
              .build();

      given(memberRepository.findByEmail(username)).willReturn(Optional.of(member));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(savedTripPlan);

      // when
      tripService.requestTripPlanGeneration(username, request, chatRoomId);

      // then
      ArgumentCaptor<TripPlan> tripPlanCaptor = ArgumentCaptor.forClass(TripPlan.class);
      verify(tripPlanRepository, times(1)).save(tripPlanCaptor.capture());

      TripPlan capturedTripPlan = tripPlanCaptor.getValue();
      assertThat(capturedTripPlan.getTitle()).isEqualTo("강릉 여행");
    }

    @Test
    @DisplayName("여러 테마를 포함한 요청을 올바르게 처리한다")
    void shouldHandleMultipleThemes() {
      // given
      String username = "test@naver.com";
      Long chatRoomId = 1L;

      Member member =
          Member.builder()
              .email(username)
              .name("테스트")
              .imageUrl("https://example.com/image.jpg")
              .provider(ProviderType.GOOGLE)
              .role(MemberRole.MEMBER)
              .build();

      List<String> multipleThemes = List.of("힐링", "맛집", "액티비티", "문화");

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .region("서울")
              .startDate(LocalDate.of(2025, 6, 1))
              .duration(4)
              .themes(multipleThemes)
              .peopleCount(3)
              .build();

      TripPlan savedTripPlan =
          TripPlan.builder()
              .member(member)
              .title("서울 여행")
              .startDate(LocalDate.of(2025, 6, 1))
              .endDate(LocalDate.of(2025, 6, 4))
              .isCompleted(false)
              .region("서울")
              .peopleCount(3)
              .build();

      given(memberRepository.findByEmail(username)).willReturn(Optional.of(member));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(savedTripPlan);

      // when
      tripService.requestTripPlanGeneration(username, request, chatRoomId);

      // then
      ArgumentCaptor<TripPlanGenerationRequestedEvent> eventCaptor =
          ArgumentCaptor.forClass(TripPlanGenerationRequestedEvent.class);
      verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

      TripPlanGenerationRequestedEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.request().themes()).hasSize(4);
      assertThat(capturedEvent.request().themes()).containsExactlyElementsOf(multipleThemes);
    }

    @Test
    @DisplayName("TripPlan의 isCompleted는 false로 초기화된다")
    void shouldInitializeTripPlanAsNotCompleted() {
      // given
      String username = "test@naver.com";
      Long chatRoomId = 1L;

      Member member =
          Member.builder()
              .email(username)
              .name("테스트")
              .imageUrl("https://example.com/image.jpg")
              .provider(ProviderType.GOOGLE)
              .role(MemberRole.MEMBER)
              .build();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .region("제주")
              .startDate(LocalDate.of(2025, 3, 1))
              .duration(3)
              .themes(List.of("힐링"))
              .peopleCount(2)
              .build();

      TripPlan savedTripPlan =
          TripPlan.builder()
              .member(member)
              .title("제주 여행")
              .startDate(LocalDate.of(2025, 3, 1))
              .endDate(LocalDate.of(2025, 3, 3))
              .isCompleted(false)
              .region("제주")
              .peopleCount(2)
              .build();

      given(memberRepository.findByEmail(username)).willReturn(Optional.of(member));
      given(tripPlanRepository.save(any(TripPlan.class))).willReturn(savedTripPlan);

      // when
      tripService.requestTripPlanGeneration(username, request, chatRoomId);

      // then
      ArgumentCaptor<TripPlan> tripPlanCaptor = ArgumentCaptor.forClass(TripPlan.class);
      verify(tripPlanRepository, times(1)).save(tripPlanCaptor.capture());

      TripPlan capturedTripPlan = tripPlanCaptor.getValue();
      assertThat(capturedTripPlan.getIsCompleted()).isFalse();
    }
  }
}
