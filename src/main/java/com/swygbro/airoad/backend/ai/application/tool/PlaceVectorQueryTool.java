package com.swygbro.airoad.backend.ai.application.tool;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.application.tool.dto.ToolResponse;
import com.swygbro.airoad.backend.content.application.PlaceVectorQueryUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class PlaceVectorQueryTool {

  private final PlaceVectorQueryUseCase placeVectorQueryUseCase;

  @Tool(
      description = """
              사용자 요구사항에 맞는 관광지를 의미적 유사도 기반으로 검색할 때 사용합니다.
              """)
  public ToolResponse searchPlace(
      @ToolParam(
              description =
                  "자연어 검색 쿼리 - 사용자의 요구사항을 자연스러운 문장으로 표현 (예: '부산에서 신선한 해산물을 먹을 수 있는 맛집을 찾고 싶어요', '가족과 함께 가기 좋은 체험형 박물관')")
          String query,
      @ToolParam(description = "반환할 최대 결과 개수 - 일반적으로 3~5개 권장") int topK,
      @ToolParam(description = "유사도 임계값 (0.0~1.0) - 높을수록 더 유사한 결과만 반환, 기본값 0.45 권장, 0.6 이상은 매우 엄격")
          double similarityThreshold) {
    log.debug(
        "장소 벡터 검색 - query: {}, topK: {}, similarityThreshold: {}",
        query,
        topK,
        similarityThreshold);

    List<Document> documents = placeVectorQueryUseCase.search(query, topK, similarityThreshold);

    log.debug(
        "검색된 장소: {}",
        documents.stream()
            .map(this::formatDocumentWithMetadata)
            .collect(Collectors.joining("\n\n")));

    if (documents.isEmpty()) {
      return ToolResponse.failure("검색 결과가 없습니다.");
    }

    return ToolResponse.success(
        documents.stream()
            .map(this::formatDocumentWithMetadata)
            .collect(Collectors.joining("\n\n")));
  }

  private String formatDocumentWithMetadata(Document doc) {
    Map<String, Object> metadata = doc.getMetadata();
    return String.format(
        """
            [장소ID: %s]
            이름: %s
            주소: %s
            테마: %s
            설명: %s
            """,
        metadata.get("placeId"),
        metadata.get("name"),
        metadata.get("address"),
        metadata.get("themes"),
        doc.getText());
  }
}
