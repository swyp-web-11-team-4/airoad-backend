package com.swygbro.airoad.backend.common.config;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 테스트용 VectorStore 설정
 *
 * <p>실제 PostgreSQL + pgvector 대신 Mock VectorStore를 제공합니다. 이를 통해 H2 in-memory DB를 사용하는 테스트 환경에서도
 * VectorStore 관련 빈들이 정상적으로 생성됩니다.
 */
@TestConfiguration
public class TestVectorStoreConfig {

  @Bean
  @Primary
  public VectorStore testVectorStore() {
    return new VectorStore() {
      @Override
      public void add(List<Document> documents) {}

      @Override
      public void delete(List<String> idList) {}

      @Override
      public void delete(Expression filterExpression) {}

      @Override
      public List<Document> similaritySearch(SearchRequest request) {
        return List.of();
      }
    };
  }
}
