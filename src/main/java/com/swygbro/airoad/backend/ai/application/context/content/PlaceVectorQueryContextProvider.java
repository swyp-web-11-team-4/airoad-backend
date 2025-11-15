package com.swygbro.airoad.backend.ai.application.context.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.application.context.dto.PlaceVectorQueryContext;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor.MetadataEntry;
import com.swygbro.airoad.backend.ai.common.context.AbstractContextProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PlaceVectorQueryContextProvider
    extends AbstractContextProvider<PlaceVectorQueryContext> {

  private final VectorStore vectorStore;

  public PlaceVectorQueryContextProvider(VectorStore vectorStore) {
    super(PlaceVectorQueryContext.class);
    this.vectorStore = vectorStore;
  }

  @Override
  protected List<MetadataEntry> doGetContext(PlaceVectorQueryContext context) {
    log.debug(
        "장소 벡터 검색 - region: {}, themes: {}, topK: {}, similarityThreshold: {}",
        context.region(),
        context.themes(),
        context.topK(),
        context.similarityThreshold());

    List<Document> places = findPlaces(context);
    List<Document> restaurants = findRestaurant(context);

    String placeInfo =
        """
        ## 장소 컨텍스트 (Place Context)

        다음의 장소 정보를 활용하여 응답하세요.

        ### 추천 장소 목록
        %s

        ### 추천 음식점 목록
        %s

        """
            .formatted(
                places.stream()
                    .map(this::formatDocumentWithMetadata)
                    .collect(Collectors.joining("\n\n")),
                restaurants.stream()
                    .map(this::formatDocumentWithMetadata)
                    .collect(Collectors.joining("\n\n")));

    return PromptMetadataAdvisor.userMetadata(placeInfo);
  }

  @Override
  public int getOrder() {
    return 20;
  }

  private List<Document> findPlaces(PlaceVectorQueryContext context) {
    List<Document> allPlaces = new ArrayList<>();

    for (String theme : context.themes()) {
      String query = context.region() + "에 있는 " + theme + " 테마에 어울리는 장소를 찾고 싶어";

      List<Document> documents =
          vectorStore.similaritySearch(
              SearchRequest.builder()
                  .query(query)
                  .topK(context.topK())
                  .similarityThreshold(context.similarityThreshold())
                  .build());

      allPlaces.addAll(documents);
    }

    // 중복 제거
    return allPlaces.stream().distinct().toList();
  }

  private List<Document> findRestaurant(PlaceVectorQueryContext context) {
    String query = context.region() + "에 있는 음식점을 찾고 싶어";

    return vectorStore.similaritySearch(
        SearchRequest.builder()
            .query(query)
            .topK(context.topK())
            .similarityThreshold(context.similarityThreshold())
            .build());
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
