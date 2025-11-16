package com.swygbro.airoad.backend.content.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaceVectorQueryServiceTest {

  @InjectMocks private PlaceVectorQueryService placeVectorQueryService;

  @Mock private VectorStore vectorStore;

  @Test
  @DisplayName("장소 검색이 요청되면 VectorStore의 similaritySearch를 호출해야 한다")
  void search가_요청되면_VectorStore의_similaritySearch를_호출해야_한다() {
    // given
    String query = "test query";
    int topK = 10;
    double similarityThreshold = 0.8;

    // when
    placeVectorQueryService.search(query, topK, similarityThreshold);

    // then
    verify(vectorStore).similaritySearch(any(SearchRequest.class));
  }
}
