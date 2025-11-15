package com.swygbro.airoad.backend.ai.application.context.ai;

import java.util.List;

import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.application.context.dto.StreamOutputContext;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor.MetadataEntry;
import com.swygbro.airoad.backend.ai.common.context.AbstractContextProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class StreamOutputProvider extends AbstractContextProvider<StreamOutputContext> {

  public StreamOutputProvider() {
    super(StreamOutputContext.class);
  }

  @Override
  protected List<MetadataEntry> doGetContext(StreamOutputContext context) {

    String outputSchema =
        """
            ## 출력 컨텍스트 (Output Context)

            응답 출력 시 지켜야 할 형식입니다. 반드시 출력 형식에 맞게 응답하세요.

            %s
            """
            .formatted(context);

    return PromptMetadataAdvisor.systemMetadata(outputSchema);
  }

  @Override
  public int getOrder() {
    return 1;
  }
}
