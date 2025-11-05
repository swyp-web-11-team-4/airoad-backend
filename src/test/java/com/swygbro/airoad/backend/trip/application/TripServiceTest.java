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

import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiConversationRepository;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
import com.swygbro.airoad.backend.fixture.chat.AiConversationFixture;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.fixture.trip.TripPlanFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.exception.MemberErrorCode;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.ChannelIdResponse;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;
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

  @Mock private AiConversationRepository aiConversationRepository;

  @InjectMocks private TripService tripService;

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
      ChannelIdResponse response = tripService.createTripPlanSession(username, request);

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
      assertThat(capturedTripPlan.getTitle()).isEmpty();
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
      assertThatThrownBy(() -> tripService.createTripPlanSession(username, request))
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
      tripService.createTripPlanSession(username, request);

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
      tripService.createTripPlanSession(username, request);

      // then
      ArgumentCaptor<TripPlan> tripPlanCaptor = ArgumentCaptor.forClass(TripPlan.class);
      verify(tripPlanRepository).save(tripPlanCaptor.capture());
      TripPlan capturedTripPlan = tripPlanCaptor.getValue();

      assertThat(capturedTripPlan.getStartDate()).isEqualTo(startDate);
      assertThat(capturedTripPlan.getEndDate()).isEqualTo(startDate);
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

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(aiConversation));

      // when
      tripService.startTripPlanGeneration(username, chatRoomId);

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
    @DisplayName("given 존재하지 않는 채팅방 ID when 여행 계획 생성 시작 then 예외가 발생한다")
    void startTripPlanGenerationWithNonExistentChatRoom() {
      // given
      String username = "test@example.com";
      Long chatRoomId = 999L;

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> tripService.startTripPlanGeneration(username, chatRoomId))
          .isInstanceOf(Exception.class);

      verify(eventPublisher, times(0)).publishEvent(any(TripPlanGenerationRequestedEvent.class));
    }

    @Test
    @DisplayName("given 여행 계획이 연결된 채팅방 when 여행 계획 생성 시작 then 올바른 여행 계획 ID로 이벤트가 발행된다")
    void startTripPlanGenerationWithValidTripPlan() throws Exception {
      // given
      String username = "test@example.com";
      Long chatRoomId = 1L;
      Long tripPlanId = 100L;

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

      given(aiConversationRepository.findById(chatRoomId)).willReturn(Optional.of(aiConversation));

      // when
      tripService.startTripPlanGeneration(username, chatRoomId);

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
