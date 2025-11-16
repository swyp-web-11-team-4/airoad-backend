package com.swygbro.airoad.backend.ai.application.context.trip;

import java.util.List;

import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.application.context.dto.TripPlanCommandContext;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor;
import com.swygbro.airoad.backend.ai.common.advisor.PromptMetadataAdvisor.MetadataEntry;
import com.swygbro.airoad.backend.ai.common.context.AbstractContextProvider;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;

import lombok.extern.slf4j.Slf4j;

/**
 * 여행 계획 생성을 위한 Command Context를 제공하는 Provider
 *
 * <p>TripPlanCommandContext로부터 지역, 기간, 테마 등의 여행 조건을 포맷팅하여 컨텍스트로 제공합니다.
 *
 * <p>CQRS 패턴의 Command 측면을 담당하며, 새로운 여행 계획 생성 시 필요한 파라미터를 AI에게 전달합니다.
 *
 * <p>특정 Agent DTO에 의존하지 않아 ChatAgent, TripAgent 등 어디서든 재사용 가능합니다.
 */
@Slf4j
@Component
public class TripPlanCommandContextProvider
    extends AbstractContextProvider<TripPlanCommandContext> {

  public TripPlanCommandContextProvider() {
    super(TripPlanCommandContext.class);
  }

  @Override
  protected List<MetadataEntry> doGetContext(TripPlanCommandContext context) {
    log.debug("여행 파라미터 포맷팅 - region: {}, duration: {}일", context.region(), context.duration());

    String formattedParameters = formatTripParameters(context);

    log.debug("여행 파라미터 포맷팅 완료 - 길이: {} 자", formattedParameters.length());

    return PromptMetadataAdvisor.systemMetadata(
        """
        ## 요구사항 컨텍스트 (Requirements Context)

        사용자가 원하는 여행 계획의 조건입니다. 여행 일정을 생성하는데 참고하세요.

        %s
        """
            .formatted(formattedParameters));
  }

  @Override
  public int getOrder() {
    return 21;
  }

  /**
   * 여행 요청 정보를 포맷팅된 문자열로 변환합니다.
   *
   * @param context 여행 계획 Command 컨텍스트
   * @return 포맷팅된 여행 파라미터 문자열
   */
  private String formatTripParameters(TripPlanCommandContext context) {
    String themeList = formatThemes(context.themes());

    return """
        ### 여행 조건
        | 항목 | 값 |
        |------|-----|
        | 지역 | %s |
        | 기간 | %d일 |
        | 시작일 | %s |
        | 종료일 | %s |
        | 인원 | %d명 |
        | 이동수단 | %s |

        ### 선호 테마
        %s

        """
        .formatted(
            context.region(),
            context.duration(),
            context.startDate(),
            context.startDate().plusDays(context.duration() - 1),
            context.peopleCount(),
            context.transportation(),
            themeList);
  }

  private String formatThemes(List<PlaceThemeType> themes) {
    return themes.stream()
        .map(PlaceThemeType::getDescription)
        .map(theme -> "- " + theme)
        .reduce((a, b) -> a + "\n" + b)
        .orElse("- 없음");
  }
}
