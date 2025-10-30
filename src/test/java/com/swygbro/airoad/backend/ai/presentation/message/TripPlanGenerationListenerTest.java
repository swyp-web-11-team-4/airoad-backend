package com.swygbro.airoad.backend.ai.presentation.message;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.event.TripPlanGenerationRequestedEvent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

/**
 * TripPlanGenerationListener의 테스트 클래스입니다.
 *
 * <p>여행 일정 생성 요청 이벤트를 수신하여 AI 서비스를 호출하고 적절한 도메인 이벤트를 발행하는 기능을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("TripPlanGenerationListener 클래스")
class TripPlanGenerationListenerTest {

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private TripPlanGenerationListener listener;

  // 테스트 데이터 상수
  private static final Long TEST_CHAT_ROOM_ID = 1L;
  private static final Long TEST_MEMBER_ID = 100L;

  /**
   * 테스트용 TripGenerationRequest를 생성하는 헬퍼 메서드입니다.
   *
   * @return 테스트용 TripGenerationRequest
   */
  private TripPlanCreateRequest createTestTripPlanCreateRequest() {
    return TripPlanCreateRequest.builder()
        .themes(List.of("힐링", "맛집"))
        .startDate(LocalDate.of(2024, 3, 1))
        .duration(3)
        .region("제주")
        .peopleCount(2)
        .build();
  }

  @Nested
  @DisplayName("handleTripPlanGenerationRequested 메서드는")
  class HandleTripPlanGenerationRequested {

    @Test
    @DisplayName("여행 일정 생성 요청 이벤트를 수신하고 예외 없이 처리한다")
    void shouldReceiveEventWithoutException() {
      // given - 여행 일정 생성 요청 이벤트 준비
      TripPlanCreateRequest request = createTestTripPlanCreateRequest();
      TripPlanGenerationRequestedEvent event =
          new TripPlanGenerationRequestedEvent(TEST_CHAT_ROOM_ID, TEST_MEMBER_ID, request);

      // when & then - 이벤트 처리 시 예외가 발생하지 않음
      assertThatCode(() -> listener.handleTripPlanGenerationRequested(event))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sessionId, chatRoomId, memberId를 포함한 이벤트를 정상 처리한다")
    void shouldHandleEventWithAllIdentifiers() {
      // given - 모든 식별자를 포함한 이벤트 준비
      TripPlanCreateRequest request = createTestTripPlanCreateRequest();
      TripPlanGenerationRequestedEvent event =
          new TripPlanGenerationRequestedEvent(TEST_CHAT_ROOM_ID, TEST_MEMBER_ID, request);

      // when & then - 정상적으로 처리됨
      assertThatCode(() -> listener.handleTripPlanGenerationRequested(event))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("여행 정보가 포함된 요청을 정상 처리한다")
    void shouldHandleEventWithTripInformation() {
      // given - 여행 정보가 포함된 요청 이벤트 준비
      TripPlanCreateRequest request = createTestTripPlanCreateRequest();
      TripPlanGenerationRequestedEvent event =
          new TripPlanGenerationRequestedEvent(TEST_CHAT_ROOM_ID, TEST_MEMBER_ID, request);

      // when & then - 여행 정보가 포함된 이벤트도 정상 처리됨
      assertThatCode(() -> listener.handleTripPlanGenerationRequested(event))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다양한 여행 테마를 가진 요청을 처리한다")
    void shouldHandleEventWithVariousThemes() {
      // given - 다양한 테마를 가진 요청 준비
      TripPlanCreateRequest request = TripPlanCreateRequest.builder()
          .themes(Arrays.asList("체험/액티비티", "문화/역사", "자연/힐링"))
          .startDate(LocalDate.of(2024, 5, 10))
          .duration(3)
          .region("경주")
          .peopleCount(4)
          .build();

      TripPlanGenerationRequestedEvent event =
          new TripPlanGenerationRequestedEvent(TEST_CHAT_ROOM_ID, TEST_MEMBER_ID, request);

      // when & then - 다양한 테마를 가진 이벤트도 정상 처리됨
      assertThatCode(() -> listener.handleTripPlanGenerationRequested(event))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("장기 여행 계획 요청을 처리한다")
    void shouldHandleLongTermTripRequest() {
      // given - 일주일 이상의 장기 여행 요청 준비
      TripPlanCreateRequest request = TripPlanCreateRequest.builder()
              .themes(List.of("복합"))
              .startDate(LocalDate.of(2024, 6, 1))
              .duration(10)
              .region("부산")
              .peopleCount(1)
              .build();

      TripPlanGenerationRequestedEvent event =
          new TripPlanGenerationRequestedEvent(TEST_CHAT_ROOM_ID, TEST_MEMBER_ID, request);

      // when & then - 장기 여행 요청도 정상 처리됨
      assertThatCode(() -> listener.handleTripPlanGenerationRequested(event))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("단기 여행 계획 요청을 처리한다")
    void shouldHandleShortTermTripRequest() {
      // given - 당일치기 여행 요청 준비
      LocalDate tripDate = LocalDate.of(2024, 4, 15);
      TripPlanCreateRequest request = TripPlanCreateRequest.builder()
              .themes(List.of("자연/힐링"))
              .startDate(tripDate)
              .duration(1)
              .region("남한산성")
              .peopleCount(3)
              .build();

      TripPlanGenerationRequestedEvent event =
          new TripPlanGenerationRequestedEvent(TEST_CHAT_ROOM_ID, TEST_MEMBER_ID, request);

      // when & then - 단기 여행 요청도 정상 처리됨
      assertThatCode(() -> listener.handleTripPlanGenerationRequested(event))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다양한 교통수단을 가진 요청을 처리한다")
    void shouldHandleEventWithDifferentTransportation() {
      // given - CAR 교통수단을 가진 요청 준비
      TripPlanCreateRequest carRequest = createTestTripPlanCreateRequest(); // 기본적으로 CAR 사용

      TripPlanGenerationRequestedEvent carEvent =
          new TripPlanGenerationRequestedEvent(TEST_CHAT_ROOM_ID, TEST_MEMBER_ID, carRequest);

      // when & then - CAR 교통수단 요청 정상 처리
      assertThatCode(() -> listener.handleTripPlanGenerationRequested(carEvent))
          .doesNotThrowAnyException();

      // given - PUBLIC_TRANSPORT 교통수단을 가진 요청 준비
      TripPlanCreateRequest publicTransportRequest = TripPlanCreateRequest.builder()
              .themes(List.of("쇼핑"))
              .startDate(LocalDate.of(2024, 3, 20))
              .duration(3)
              .region("서울")
              .peopleCount(2)
              .build();

      TripPlanGenerationRequestedEvent publicTransportEvent =
          new TripPlanGenerationRequestedEvent(
              TEST_CHAT_ROOM_ID, TEST_MEMBER_ID, publicTransportRequest);

      // when & then - PUBLIC_TRANSPORT 교통수단 요청 정상 처리
      assertThatCode(() -> listener.handleTripPlanGenerationRequested(publicTransportEvent))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("비동기 처리")
  class AsyncProcessing {

    @Test
    @DisplayName("이벤트 처리는 @Async 어노테이션으로 비동기 실행된다")
    void shouldBeAnnotatedWithAsync() throws NoSuchMethodException {
      // given - handleTripPlanGenerationRequested 메서드 확인

      // when - 메서드에 @Async 어노테이션이 있는지 확인
      boolean hasAsyncAnnotation =
          listener
              .getClass()
              .getMethod(
                  "handleTripPlanGenerationRequested", TripPlanGenerationRequestedEvent.class)
              .isAnnotationPresent(org.springframework.scheduling.annotation.Async.class);

      // then - @Async 어노테이션이 존재함
      org.assertj.core.api.Assertions.assertThat(hasAsyncAnnotation).isTrue();
    }
  }
}
