package com.swygbro.airoad.backend.content.infrastructure.repository;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** PgVectorStore 래퍼 Repository */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PlaceVectorStoreRepository {

  private final VectorStore vectorStore;

  /**
   * Document를 벡터 스토어에 저장
   *
   * @param document 저장할 Document
   */
  public void save(Document document) {
    vectorStore.add(List.of(document));
    log.info("Successfully saved document with ID: {}", document.getId());
  }

  /**
   * 여러 Document를 벡터 스토어에 일괄 저장
   *
   * @param documents 저장할 Document 리스트
   */
  public void saveAll(List<Document> documents) {
    if (!documents.isEmpty()) {
      vectorStore.add(documents);
      log.info("Successfully saved {} documents", documents.size());
    }
  }

  /**
   * Place ID로 Document 삭제
   *
   * <p>메타데이터의 placeId로 필터링하여 삭제합니다.
   *
   * @param placeId 삭제할 Place의 ID
   */
  public void deleteByPlaceId(Long placeId) {
    try {
      log.info("Deleting document by placeId: {}", placeId);
      FilterExpressionBuilder b = new FilterExpressionBuilder();
      Filter.Expression filterExpression = b.eq("placeId", placeId).build();
      vectorStore.delete(filterExpression);
      log.info("Successfully deleted document by placeId: {}", placeId);
    } catch (Exception e) {
      log.error("Failed to delete document by placeId: {}", placeId, e);
    }
  }

  /**
   * 쿼리와 유사한 Document를 검색
   *
   * @param query 검색 쿼리
   * @param topK 반환할 상위 K개 결과
   * @return 유사도 순으로 정렬된 Document 리스트
   */
  public List<Document> similaritySearch(String query, int topK) {
    SearchRequest request = SearchRequest.builder().query(query).topK(topK).build();
    return vectorStore.similaritySearch(request);
  }
}
