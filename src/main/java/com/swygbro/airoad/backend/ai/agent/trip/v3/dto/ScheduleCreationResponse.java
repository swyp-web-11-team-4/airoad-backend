package com.swygbro.airoad.backend.ai.agent.trip.v3.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;

import lombok.Builder;

@Builder
public record ScheduleCreationResponse(
    @JsonPropertyDescription(
            "해당 일차의 소제목, 다른 소제목과 중복되지 않게 작성하고 재밌고 재치있게 작성하세요. 예: '오늘은 내가 만수르야!', '한옥 골목 속 숨은 서울 찾기'")
        String title,
    @JsonPropertyDescription(
            """
        여행 일정 전체 설명입니다. 마크다운 형식으로 작성하되 다음 규칙을 준수하세요:
        - 쌍따옴표(") 사용 금지, 작은따옴표(') 사용
        - ID 값 작성 금지
        - 유저가 이해하기 쉽도록 BulletPoint + Nutshell 사용
        - 일정 카테고리는 반드시 한글로 변환
        - 챗봇이 말하는 듯한 톤으로 자연어로 풀어서 설명 작성

        ## 작성 형식
        다음의 마크다운 템플릿을 준수해서 작성하세요.
        ```markdown
        **n일차 - {제목}**
        - **{일정 카테고리}**: {장소 이름}
          {요약 설명}
        - **{일정 카테고리}**: {장소 이름}
          {요약 설명}
        - **{일정 카테고리}**: {장소 이름}
          {요약 설명}

        {전체 일정 요약}: Optional
        ```

        ## 출력 예시
        ```markdown
        **1일차 - 제주 동부 해안 탐방**
        - **오전 일정**: 성산일출봉
          제주를 대표하는 일출 명소로, 유네스코 세계자연유산입니다. 정상까지 약 30분 소요됩니다.
        - **오후 일정**: 섭지코지
          드라마 촬영지로 유명한 아름다운 해안가입니다. 카페에서 바다를 바라보며 휴식을 취할 수 있습니다.
        - **저녁 일정**: 성산 해녀의집
          신선한 해산물 정식을 맛볼 수 있는 현지 맛집입니다.

        제주의 동부 해안을 따라 자연 경관을 감상하며 여유로운 하루를 보냅니다.
        ```
        """)
        String description,
    @JsonPropertyDescription("일정에 포함될 장소 목록") List<ScheduledPlaceInfo> places) {

  @Builder
  public record ScheduledPlaceInfo(
      @JsonPropertyDescription("DB에 저장된 장소 식별자 ID, null 값을 가질 수 없음") Long placeId,
      @JsonPropertyDescription(
              """
          장소에 대한 설명 및 선정 이유를 150~200자로 작성
          - 예시: "한라수목원은 자연형 수목원으로 한라산 자생 식물과 숲속 산책로를 즐길 수 있는 힐링 명소입니다. 도심과 가까워 여행 중 여유롭게 산책하기 좋은 장소입니다."
          """)
          String summary,
      @JsonPropertyDescription("일정 방문 순서 (1부터 시작)") int visitOrder,
      @JsonPropertyDescription("일정 카테고리") ScheduledCategory category) {}
}
