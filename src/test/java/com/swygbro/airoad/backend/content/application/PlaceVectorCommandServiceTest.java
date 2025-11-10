package com.swygbro.airoad.backend.content.application;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.content.domain.converter.PlaceDocumentConverter;
import com.swygbro.airoad.backend.content.domain.dto.request.PlaceVectorSaveRequest;
import com.swygbro.airoad.backend.content.infrastructure.repository.PlaceVectorStoreRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PlaceVectorCommandServiceTest {

  @Mock private PlaceVectorStoreRepository vectorStoreRepository;

  @Mock private PlaceDocumentConverter placeDocumentConverter;

  @InjectMocks private PlaceVectorCommandService placeVectorCommandService;

  @Nested
  @DisplayName("장소 벡터 저장 요청 시")
  class SavePlaceVector {

    @Test
    @DisplayName("기존 벡터를 삭제하고 새로운 벡터를 저장한다")
    void givenRequest_whenSave_thenDeleteOldAndSaveNew() {
      // given: 벡터 저장 요청
      PlaceVectorSaveRequest request =
          PlaceVectorSaveRequest.builder()
              .placeId(1L)
              .name("서울역")
              .address("서울특별시 용산구")
              .content("서울특별시 용산구에 위치한 서울역은 서울의 중심 역입니다.")
              .themes(List.of("교통", "관광"))
              .build();

      // given: 메타데이터 생성
      Map<String, Object> metadata = Map.of("placeId", 1L, "name", "서울역", "address", "서울특별시 용산구");
      given(
              placeDocumentConverter.buildMetadataFromEvent(
                  request.placeId(), request.name(), request.address(), request.themes()))
          .willReturn(metadata);

      // when: 벡터 저장 요청
      placeVectorCommandService.savePlaceVector(request);

      // then: 기존 벡터 삭제됨
      then(vectorStoreRepository).should(times(1)).deleteByPlaceId(1L);

      // then: 새로운 Document 저장됨
      ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
      then(vectorStoreRepository).should(times(1)).save(documentCaptor.capture());

      // then: Document 내용 검증
      Document savedDocument = documentCaptor.getValue();
      assertThat(savedDocument.getText()).isEqualTo(request.content());
      assertThat(savedDocument.getMetadata()).containsEntry("placeId", 1L);
    }

    @Test
    @DisplayName("메타데이터가 정상적으로 생성되어 저장된다")
    void givenRequest_whenSave_thenMetadataIsCorrectlyBuilt() {
      // given: 벡터 저장 요청
      PlaceVectorSaveRequest request =
          PlaceVectorSaveRequest.builder()
              .placeId(2L)
              .name("강남역")
              .address("서울특별시 강남구")
              .content("강남역 설명")
              .themes(List.of("쇼핑"))
              .build();

      // given: 메타데이터 생성
      Map<String, Object> metadata = Map.of("placeId", 2L);
      given(
              placeDocumentConverter.buildMetadataFromEvent(
                  eq(2L), eq("강남역"), eq("서울특별시 강남구"), eq(List.of("쇼핑"))))
          .willReturn(metadata);

      // when: 벡터 저장 요청
      placeVectorCommandService.savePlaceVector(request);

      // then: Converter가 올바른 인자로 호출됨
      then(placeDocumentConverter)
          .should(times(1))
          .buildMetadataFromEvent(2L, "강남역", "서울특별시 강남구", List.of("쇼핑"));
    }

    @Test
    @DisplayName("VectorStore 저장 실패 시 예외를 발생시킨다")
    void givenRepositoryFailure_whenSave_thenThrowException() {
      // given: 벡터 저장 요청
      PlaceVectorSaveRequest request =
          PlaceVectorSaveRequest.builder()
              .placeId(3L)
              .name("제주공항")
              .address("제주특별자치도")
              .content("제주공항 설명")
              .themes(List.of("교통"))
              .build();

      // given: 메타데이터 생성
      Map<String, Object> metadata = Map.of("placeId", 3L);
      given(placeDocumentConverter.buildMetadataFromEvent(any(), any(), any(), any()))
          .willReturn(metadata);

      // given: Repository 저장 실패
      willThrow(new RuntimeException("DB connection failed"))
          .given(vectorStoreRepository)
          .save(any(Document.class));

      // when & then: 예외 발생
      assertThatThrownBy(() -> placeVectorCommandService.savePlaceVector(request))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Failed to save document to VectorStore");
    }
  }
}
