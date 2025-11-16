package com.swygbro.airoad.backend.ai.application.context.content;

import java.util.List;

import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.application.context.dto.PlaceQueryContext;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor.MetadataEntry;
import com.swygbro.airoad.backend.ai.common.context.AbstractContextProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PlaceQueryContextProvider extends AbstractContextProvider<PlaceQueryContext> {

  public PlaceQueryContextProvider() {
    super(PlaceQueryContext.class);
  }

  @Override
  public int getOrder() {
    return 30;
  }

  @Override
  protected List<MetadataEntry> doGetContext(PlaceQueryContext context) {
    log.debug(
        "장소 정보 제공 - name: {}, address: {}, description: {}, operatingHours: {}, holidayInfo: {}, themes: {}",
        context.name(),
        context.address(),
        context.description(),
        context.operatingHours(),
        context.holidayInfo(),
        context.themes());

    String placeInfo =
        """
        ## 장소 컨텍스트 (Place Context)

        다음의 장소 정보를 활용하여 응답하세요.

        - 이름: %s
        - 주소: %s
        - 설명: %s
        - 운영 시간 정보: %s
        - 휴무일 정보: %s
        - 테마: %s

        """
            .formatted(
                context.name(),
                context.address(),
                context.description(),
                context.operatingHours(),
                context.holidayInfo(),
                context.themes());

    return PromptMetadataAdvisor.systemMetadata(placeInfo);
  }
}
