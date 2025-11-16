package com.swygbro.airoad.backend.ai.application.context.content;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
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
        "장소 유사도 검색 - queryType: {}, searchRequest: {}",
        context.queryType(),
        context.searchRequest());

    List<Document> documents = vectorSearch(context);

    String documentContext =
        """
        ## %s 컨텍스트 (%s Context)

        다음 컨텍스트 정보를 사용하여 사용자 질문에 답변하세요.
        절대로 컨텍스트에 없는 내용을 지어내서 답변하면 안 됩니다.

        %s

        """
            .formatted(
                context.queryType().getDescription(),
                context.queryType().name(),
                documents.stream()
                    .map(this::formatDocumentWithMetadata)
                    .collect(Collectors.joining("\n\n")));

    return PromptMetadataAdvisor.systemMetadata(documentContext);
  }

  @Override
  public int getOrder() {
    return 31;
  }

  private List<Document> vectorSearch(PlaceVectorQueryContext context) {
    List<Document> allPlaces = vectorStore.similaritySearch(context.searchRequest());
    return allPlaces.stream().distinct().toList();
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
