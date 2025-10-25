package com.swygbro.airoad.backend.content.application;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PlaceEmbeddingSchedulerTest {

  @Mock private PlaceEmbeddingUseCase placeEmbeddingUseCase;

  @InjectMocks private PlaceEmbeddingScheduler placeEmbeddingScheduler;

  @Nested
  class 일일_증분_임베딩_스케줄_실행_시 {

    @Test
    void 매일_실행되면_최근_24시간_수정된_Place를_임베딩할_수_있다() {
      // given - 현재 시간 기준 설정
      LocalDateTime beforeCall = LocalDateTime.now(ZoneOffset.UTC).minusHours(24);

      // when - 일일 스케줄러 실행
      placeEmbeddingScheduler.embedModifiedPlacesDaily();

      // then - 24시간 전 기준으로 수정된 Place 임베딩 요청
      ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
      verify(placeEmbeddingUseCase, times(1)).embedModifiedPlaces(captor.capture());

      LocalDateTime capturedTime = captor.getValue();
      LocalDateTime afterCall = LocalDateTime.now(ZoneOffset.UTC).minusHours(24);

      // 24시간 전 시각이 정확히 계산되었는지 검증 (오차 1초 이내)
      assertThatCode(
              () -> {
                if (capturedTime.isBefore(beforeCall.minusSeconds(1))
                    || capturedTime.isAfter(afterCall.plusSeconds(1))) {
                  throw new AssertionError("Captured time is not within expected range");
                }
              })
          .doesNotThrowAnyException();
    }

    @Test
    void 서버_시간대와_무관하게_UTC_기준으로_동작해야_한다() {
      // given - UTC 기준 시간 계산
      LocalDateTime expectedTime = LocalDateTime.now(ZoneOffset.UTC).minusHours(24);

      // when - 일일 스케줄러 실행
      placeEmbeddingScheduler.embedModifiedPlacesDaily();

      // then - UTC 기준 시간으로 임베딩 요청
      ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
      verify(placeEmbeddingUseCase, times(1)).embedModifiedPlaces(captor.capture());

      LocalDateTime capturedTime = captor.getValue();

      // UTC 기준으로 계산되었는지 확인 (오차 범위 2초)
      long secondsDifference =
          Math.abs(
              capturedTime.toEpochSecond(ZoneOffset.UTC)
                  - expectedTime.toEpochSecond(ZoneOffset.UTC));
      assertThatCode(
              () -> {
                if (secondsDifference > 2) {
                  throw new AssertionError(
                      "Time difference exceeds 2 seconds: " + secondsDifference);
                }
              })
          .doesNotThrowAnyException();
    }

    @Test
    void 임베딩_실패_시에도_스케줄러는_중단되지_않아야_한다() {
      // given - embedModifiedPlaces가 예외를 던지도록 설정
      willThrow(new RuntimeException("Embedding 처리 중 오류 발생"))
          .given(placeEmbeddingUseCase)
          .embedModifiedPlaces(any(LocalDateTime.class));

      // when & then - 예외가 발생해도 스케줄러는 안전하게 종료됨
      assertThatCode(() -> placeEmbeddingScheduler.embedModifiedPlacesDaily())
          .doesNotThrowAnyException();

      // embedModifiedPlaces 호출은 시도되었는지 확인
      verify(placeEmbeddingUseCase, times(1)).embedModifiedPlaces(any(LocalDateTime.class));
    }
  }

  @Nested
  class 주간_전체_재임베딩_스케줄_실행_시 {

    @Test
    void 주간_실행되면_전체_Place를_재임베딩할_수_있다() {
      // when - 주간 스케줄러 실행
      placeEmbeddingScheduler.embedAllPlacesWeekly();

      // then - 전체 Place 재임베딩 요청
      verify(placeEmbeddingUseCase, times(1)).embedAllPlaces();
    }

    @Test
    void 재임베딩_실패_시에도_스케줄러는_중단되지_않아야_한다() {
      // given - embedAllPlaces가 예외를 던지도록 설정
      willThrow(new RuntimeException("전체 임베딩 처리 중 오류 발생"))
          .given(placeEmbeddingUseCase)
          .embedAllPlaces();

      // when & then - 예외가 발생해도 스케줄러는 안전하게 종료됨
      assertThatCode(() -> placeEmbeddingScheduler.embedAllPlacesWeekly())
          .doesNotThrowAnyException();

      // embedAllPlaces 호출은 시도되었는지 확인
      verify(placeEmbeddingUseCase, times(1)).embedAllPlaces();
    }
  }
}
