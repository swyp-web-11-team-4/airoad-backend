package com.swygbro.airoad.backend.content.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import lombok.Setter;

public class FakeVectorStore implements VectorStore {

  private final Map<String, Document> documents = new HashMap<>();

  @Override
  public void add(List<Document> documents) {
    for (Document doc : documents) {
      this.documents.put(doc.getId(), doc);
    }
  }

  @Override
  public void delete(List<String> idList) {
    idList.forEach(documents::remove);
  }

  @Override
  public void delete(Filter.Expression filterExpression) {}

  @Override
  public List<Document> similaritySearch(SearchRequest request) {
    return new ArrayList<>(documents.values()).stream().limit(request.getTopK()).toList();
  }

  public void clear() {
    documents.clear();
  }

  @Setter
  static class FakeDocument extends Document {
    private Double score;

    public FakeDocument(String id, String content, Map<String, Object> metadata) {
      super(id, content, metadata);
      this.score = 0.0;
    }

    @Override
    public Double getScore() {
      return score;
    }
  }
}
