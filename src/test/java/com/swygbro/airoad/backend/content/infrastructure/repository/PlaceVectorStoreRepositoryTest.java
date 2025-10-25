package com.swygbro.airoad.backend.content.infrastructure.repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PlaceVectorStoreRepositoryTest {

  @Mock private VectorStore vectorStore;

  @InjectMocks private PlaceVectorStoreRepository placeVectorStoreRepository;

  @Nested
  class 단일_Document_저장_시 {

    @Test
    void 단일_Document를_저장할_수_있다() {
      // given - 저장할 Document 생성
      Document testDocument =
          new Document("test-id", "테스트 장소 설명", Map.of("placeId", 1L, "name", "테스트 장소"));

      // when - 단일 Document 저장 요청
      placeVectorStoreRepository.save(testDocument);

      // then - vectorStore에 List 형태로 전달되어 저장됨
      ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
      verify(vectorStore, times(1)).add(captor.capture());

      List<Document> capturedDocuments = captor.getValue();
      assertThat(capturedDocuments).hasSize(1);
      assertThat(capturedDocuments.get(0)).isEqualTo(testDocument);
    }
  }

  @Nested
  class 여러_Document_저장_시 {

    @Test
    void 여러_Document를_한_번에_저장할_수_있다() {
      // given - 여러 Document 생성
      Document doc1 = new Document("id-1", "장소1 설명", Map.of("placeId", 1L));
      Document doc2 = new Document("id-2", "장소2 설명", Map.of("placeId", 2L));
      Document doc3 = new Document("id-3", "장소3 설명", Map.of("placeId", 3L));
      List<Document> documents = List.of(doc1, doc2, doc3);

      // when - 여러 Document 일괄 저장 요청
      placeVectorStoreRepository.saveAll(documents);

      // then - vectorStore에 모든 Document가 전달되어 저장됨
      ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
      verify(vectorStore, times(1)).add(captor.capture());

      List<Document> capturedDocuments = captor.getValue();
      assertThat(capturedDocuments).hasSize(3);
      assertThat(capturedDocuments).containsExactly(doc1, doc2, doc3);
    }

    @Test
    void 빈_리스트로_호출하면_불필요한_처리를_하지_않는다() {
      // given - 빈 Document 리스트
      List<Document> emptyDocuments = Collections.emptyList();

      // when - 빈 리스트로 저장 요청
      placeVectorStoreRepository.saveAll(emptyDocuments);

      // then - vectorStore 호출 없이 안전하게 종료
      verify(vectorStore, never()).add(anyList());
    }
  }

  @Nested
  class Place_ID로_삭제_시 {

    @Test
    void Place_ID로_해당_임베딩을_삭제할_수_있다() {
      // given - 삭제할 Place ID
      Long testPlaceId = 100L;

      // when - Place ID로 삭제 요청
      placeVectorStoreRepository.deleteByPlaceId(testPlaceId);

      // then - Place ID 필터를 적용하여 vectorStore에서 삭제
      ArgumentCaptor<Filter.Expression> captor = ArgumentCaptor.forClass(Filter.Expression.class);
      verify(vectorStore, times(1)).delete(captor.capture());

      // Filter.Expression이 정상적으로 생성되었는지 확인
      assertThat(captor.getValue()).isNotNull();
    }

    @Test
    void 삭제_중_오류가_발생해도_안전하게_처리할_수_있다() {
      // given - vectorStore에서 삭제 중 예외 발생 설정
      willThrow(new RuntimeException("Vector store delete error"))
          .given(vectorStore)
          .delete(any(Filter.Expression.class));

      // when & then - 예외가 발생해도 메서드는 안전하게 종료됨
      assertThatCode(() -> placeVectorStoreRepository.deleteByPlaceId(1L))
          .doesNotThrowAnyException();

      // vectorStore 삭제 시도는 수행되었는지 확인
      verify(vectorStore, times(1)).delete(any(Filter.Expression.class));
    }
  }

  @Nested
  class 유사도_검색_시 {

    @Test
    void 쿼리_텍스트로_유사한_Place를_검색할_수_있다() {
      // given - 검색 쿼리와 결과 개수 설정
      String query = "서울 맛집 추천";
      int topK = 5;
      List<Document> expectedResults =
          List.of(
              new Document("id-1", "강남 맛집", Map.of("placeId", 1L)),
              new Document("id-2", "홍대 맛집", Map.of("placeId", 2L)));
      given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(expectedResults);

      // when - 쿼리로 유사도 검색 요청
      List<Document> results = placeVectorStoreRepository.similaritySearch(query, topK);

      // then - 유사한 Place Document 목록 반환
      assertThat(results).hasSize(2);
      assertThat(results).isEqualTo(expectedResults);

      // then - 올바른 파라미터로 vectorStore 호출됨
      ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
      verify(vectorStore, times(1)).similaritySearch(captor.capture());

      SearchRequest capturedRequest = captor.getValue();
      assertThat(capturedRequest).isNotNull();
      assertThat(capturedRequest.getQuery()).isEqualTo(query);
      assertThat(capturedRequest.getTopK()).isEqualTo(topK);
    }
  }
}
