package com.swygbro.airoad.backend.content.presentation.web;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.content.application.PlaceEmbeddingUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PlaceVectorControllerTest {

  @Mock private PlaceEmbeddingUseCase placeEmbeddingUseCase;

  @InjectMocks private PlaceVectorController placeVectorController;

  @Nested
  @DisplayName("장소 벡터 생성 요청 시")
  class CreatePlacesVector {

    @Test
    @DisplayName("since 파라미터 없이 요청하면 모든 장소를 임베딩한다")
    void givenNoSinceParam_whenRequested_thenEmbedAllPlaces() {
      // when: since 파라미터 없이 요청
      ResponseEntity<?> response = placeVectorController.createPlacesVector(null);

      // then: 모든 장소 임베딩이 수행됨
      then(placeEmbeddingUseCase).should(times(1)).embedAllPlaces();
      then(placeEmbeddingUseCase)
          .should(times(0))
          .embedModifiedPlaces(org.mockito.ArgumentMatchers.any());

      // then: 204 No Content 응답
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("since 파라미터와 함께 요청하면 해당 시간 이후 수정된 장소만 임베딩한다")
    void givenSinceParam_whenRequested_thenEmbedModifiedPlaces() {
      // given: 특정 시간
      LocalDateTime since = LocalDateTime.of(2025, 1, 1, 0, 0);

      // when: since 파라미터와 함께 요청
      ResponseEntity<?> response = placeVectorController.createPlacesVector(since);

      // then: 지정된 시간 이후 수정된 장소만 임베딩이 수행됨
      then(placeEmbeddingUseCase).should(times(1)).embedModifiedPlaces(since);
      then(placeEmbeddingUseCase).should(times(0)).embedAllPlaces();

      // then: 204 No Content 응답
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
  }

  @Nested
  @DisplayName("특정 장소 벡터 생성 요청 시")
  class CreatePlaceVector {

    @Test
    @DisplayName("placeId를 전달하면 해당 장소만 임베딩한다")
    void givenPlaceId_whenRequested_thenEmbedSpecificPlace() {
      // given: 장소 ID
      Long placeId = 123L;

      // when: 특정 장소 임베딩 요청
      ResponseEntity<?> response = placeVectorController.createPlaceVector(placeId);

      // then: 해당 장소 임베딩이 수행됨
      then(placeEmbeddingUseCase).should(times(1)).embedPlace(placeId);

      // then: 204 No Content 응답
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
  }
}
