package com.swygbro.airoad.backend.content.presentation.message;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.ai.domain.event.PlaceSummaryGeneratedEvent;
import com.swygbro.airoad.backend.content.application.PlaceVectorCommandUseCase;
import com.swygbro.airoad.backend.content.domain.dto.request.PlaceVectorSaveRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PlaceVectorStoreListenerTest {

  @Mock private PlaceVectorCommandUseCase placeVectorCommandUseCase;

  @InjectMocks private PlaceVectorStoreListener listener;

  @Nested
  @DisplayName("장소 요약 생성 완료 이벤트 수신 시")
  class OnPlaceSummaryGenerated {

    @Test
    @DisplayName("이벤트 정보를 기반으로 벡터 스토어에 저장 요청을 한다")
    void givenEvent_whenReceived_thenSaveToVectorStore() {
      // given: 장소 요약 생성 완료 이벤트
      PlaceSummaryGeneratedEvent event =
          new PlaceSummaryGeneratedEvent(
              1L, "서울역", "서울특별시 용산구", List.of("교통", "관광"), "서울특별시 용산구에 위치한 서울역은 서울의 중심 역입니다.");

      // when: 이벤트 수신
      listener.onPlaceSummaryGenerated(event);

      // then: 벡터 저장 요청이 호출됨
      ArgumentCaptor<PlaceVectorSaveRequest> requestCaptor =
          ArgumentCaptor.forClass(PlaceVectorSaveRequest.class);
      then(placeVectorCommandUseCase).should(times(1)).savePlaceVector(requestCaptor.capture());

      // then: 이벤트 정보가 요청에 매핑됨
      PlaceVectorSaveRequest capturedRequest = requestCaptor.getValue();
      assertThat(capturedRequest.placeId()).isEqualTo(1L);
      assertThat(capturedRequest.name()).isEqualTo("서울역");
      assertThat(capturedRequest.address()).isEqualTo("서울특별시 용산구");
      assertThat(capturedRequest.themes()).containsExactly("교통", "관광");
      assertThat(capturedRequest.content()).isEqualTo("서울특별시 용산구에 위치한 서울역은 서울의 중심 역입니다.");
    }

    @Test
    @DisplayName("테마가 없는 경우에도 요청을 처리한다")
    void givenEventWithoutThemes_whenReceived_thenSaveToVectorStore() {
      // given: 테마가 없는 이벤트
      PlaceSummaryGeneratedEvent event =
          new PlaceSummaryGeneratedEvent(2L, "강남역", "서울특별시 강남구", List.of(), "강남역 설명");

      // when: 이벤트 수신
      listener.onPlaceSummaryGenerated(event);

      // then: 벡터 저장 요청이 호출됨
      ArgumentCaptor<PlaceVectorSaveRequest> requestCaptor =
          ArgumentCaptor.forClass(PlaceVectorSaveRequest.class);
      then(placeVectorCommandUseCase).should(times(1)).savePlaceVector(requestCaptor.capture());

      // then: 빈 테마 리스트로 전달됨
      PlaceVectorSaveRequest capturedRequest = requestCaptor.getValue();
      assertThat(capturedRequest.themes()).isEmpty();
    }

    @Test
    @DisplayName("여러 테마가 있는 경우 모두 정확하게 전달된다")
    void givenEventWithMultipleThemes_whenReceived_thenAllThemesArePassedCorrectly() {
      // given: 여러 테마가 있는 이벤트
      List<String> themes = List.of("관광", "맛집", "쇼핑", "문화");
      PlaceSummaryGeneratedEvent event =
          new PlaceSummaryGeneratedEvent(3L, "명동", "서울특별시 중구", themes, "명동 설명");

      // when: 이벤트 수신
      listener.onPlaceSummaryGenerated(event);

      // then: 모든 테마가 전달됨
      ArgumentCaptor<PlaceVectorSaveRequest> requestCaptor =
          ArgumentCaptor.forClass(PlaceVectorSaveRequest.class);
      then(placeVectorCommandUseCase).should(times(1)).savePlaceVector(requestCaptor.capture());

      PlaceVectorSaveRequest capturedRequest = requestCaptor.getValue();
      assertThat(capturedRequest.themes()).containsExactlyElementsOf(themes);
    }
  }
}
