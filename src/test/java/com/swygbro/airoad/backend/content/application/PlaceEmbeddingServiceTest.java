package com.swygbro.airoad.backend.content.application;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

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

import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.content.domain.event.PlaceSummaryRequestedEvent;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceRepository;
import com.swygbro.airoad.backend.fixture.content.PlaceFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PlaceEmbeddingServiceTest {

  @Mock private PlaceRepository placeRepository;

  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private PlaceEmbeddingService placeEmbeddingService;

  @Nested
  @DisplayName("모든 장소 임베딩 요청 시")
  class EmbedAllPlaces {

    @Test
    @DisplayName("모든 장소를 조회하고 필수 방문지만 이벤트를 발행한다")
    void givenPlaces_whenEmbedAll_thenPublishEventsForMustVisit() {
      // given: 필수 방문지와 일반 장소
      Place mustVisitPlace = PlaceFixture.createMustVisit();
      Place normalPlace = PlaceFixture.createWithMinimalInfo();

      given(placeRepository.streamAllBy()).willReturn(Stream.of(mustVisitPlace, normalPlace));

      // when: 모든 장소 임베딩 요청
      placeEmbeddingService.embedAllPlaces();

      // then: 필수 방문지에 대해서만 이벤트 발행됨
      then(eventPublisher).should(times(1)).publishEvent(any(PlaceSummaryRequestedEvent.class));
    }

    @Test
    @DisplayName("장소가 없으면 이벤트를 발행하지 않는다")
    void givenNoPlaces_whenEmbedAll_thenNoEventsPublished() {
      // given: 빈 스트림
      given(placeRepository.streamAllBy()).willReturn(Stream.empty());

      // when: 모든 장소 임베딩 요청
      placeEmbeddingService.embedAllPlaces();

      // then: 이벤트 발행되지 않음
      then(eventPublisher).should(never()).publishEvent(any(PlaceSummaryRequestedEvent.class));
    }
  }

  @Nested
  @DisplayName("수정된 장소 임베딩 요청 시")
  class EmbedModifiedPlaces {

    @Test
    @DisplayName("특정 시간 이후 수정된 장소만 조회하고 필수 방문지만 이벤트를 발행한다")
    void givenModifiedPlaces_whenEmbedModified_thenPublishEventsForMustVisit() {
      // given: 기준 시간
      LocalDateTime since = LocalDateTime.of(2025, 1, 1, 0, 0);

      // given: 필수 방문지
      Place mustVisitPlace = PlaceFixture.createMustVisit();

      given(placeRepository.streamByUpdatedAtAfter(since)).willReturn(Stream.of(mustVisitPlace));

      // when: 수정된 장소 임베딩 요청
      placeEmbeddingService.embedModifiedPlaces(since);

      // then: 필수 방문지에 대해서만 이벤트 발행됨
      then(eventPublisher).should(times(1)).publishEvent(any(PlaceSummaryRequestedEvent.class));
    }

    @Test
    @DisplayName("수정된 장소가 없으면 이벤트를 발행하지 않는다")
    void givenNoModifiedPlaces_whenEmbedModified_thenNoEventsPublished() {
      // given: 기준 시간
      LocalDateTime since = LocalDateTime.of(2025, 1, 1, 0, 0);
      given(placeRepository.streamByUpdatedAtAfter(since)).willReturn(Stream.empty());

      // when: 수정된 장소 임베딩 요청
      placeEmbeddingService.embedModifiedPlaces(since);

      // then: 이벤트 발행되지 않음
      then(eventPublisher).should(never()).publishEvent(any(PlaceSummaryRequestedEvent.class));
    }
  }

  @Nested
  @DisplayName("특정 장소 임베딩 요청 시")
  class EmbedPlace {

    @Test
    @DisplayName("장소를 조회하고 이벤트를 발행한다")
    void givenPlaceId_whenEmbedPlace_thenPublishEvent() {
      // given: 장소 ID
      Long placeId = 1L;
      Place place = PlaceFixture.withId(placeId, PlaceFixture.createWithFullInfo());

      given(placeRepository.findByIdWithThemes(placeId)).willReturn(Optional.of(place));

      // when: 특정 장소 임베딩 요청
      placeEmbeddingService.embedPlace(placeId);

      // then: 이벤트 발행됨
      ArgumentCaptor<PlaceSummaryRequestedEvent> eventCaptor =
          ArgumentCaptor.forClass(PlaceSummaryRequestedEvent.class);
      then(eventPublisher).should(times(1)).publishEvent(eventCaptor.capture());

      // then: 이벤트 내용 검증
      PlaceSummaryRequestedEvent event = eventCaptor.getValue();
      assertThat(event.placeId()).isEqualTo(placeId);
      assertThat(event.name()).isEqualTo(place.getLocation().getName());
      assertThat(event.address()).isEqualTo(place.getLocation().getAddress());
    }

    @Test
    @DisplayName("장소가 존재하지 않으면 예외를 발생시킨다")
    void givenNonExistentPlaceId_whenEmbedPlace_thenThrowException() {
      // given: 존재하지 않는 장소 ID
      Long placeId = 999L;
      given(placeRepository.findByIdWithThemes(placeId)).willReturn(Optional.empty());

      // when & then: 예외 발생
      assertThatThrownBy(() -> placeEmbeddingService.embedPlace(placeId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Place not found with ID: " + placeId);

      // then: 이벤트 발행되지 않음
      then(eventPublisher).should(never()).publishEvent(any(PlaceSummaryRequestedEvent.class));
    }
  }
}
