package com.swygbro.airoad.backend.content.application;

import java.util.List;

import org.springframework.ai.document.Document;

public interface PlaceVectorQueryUseCase {

  List<Document> search(String query, int topK, double similarityThreshold);
}
