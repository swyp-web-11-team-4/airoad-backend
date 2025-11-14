package com.swygbro.airoad.backend.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("!test")
public class VectorStoreConfig {

  @Value("${spring.ai.vectorstore.pgvector.table-name:place_embedding}")
  private String tableName;

  @Value("${spring.ai.vectorstore.pgvector.dimensions:1024}")
  private Integer dimensions;

  @Value("${spring.ai.vectorstore.pgvector.schema-name:public}")
  private String schemaName;

  @Bean
  public VectorStore vectorStore(
      JdbcTemplate jdbcTemplate, @Qualifier("naverEmbeddingModel") EmbeddingModel embeddingModel) {
    return PgVectorStore.builder(jdbcTemplate, embeddingModel)
        .dimensions(dimensions)
        .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
        .indexType(PgVectorStore.PgIndexType.HNSW)
        .initializeSchema(true)
        .schemaName(schemaName)
        .vectorTableName(tableName)
        .build();
  }
}
