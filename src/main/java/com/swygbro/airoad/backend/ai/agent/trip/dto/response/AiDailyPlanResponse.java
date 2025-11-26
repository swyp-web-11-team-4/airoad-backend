package com.swygbro.airoad.backend.ai.agent.trip.dto.response;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.swygbro.airoad.backend.trip.domain.entity.ScheduledCategory;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;

import lombok.Builder;

/** AI가 생성한 일별 계획 DTO */
@Builder
public record AiDailyPlanResponse(
    @JsonPropertyDescription("1부터 N까지 순차적으로 증가하는 일차 번호") int dayNumber,
    @JsonPropertyDescription("'yyyy-MM-dd' 형식의 날짜") LocalDate date,
    @JsonPropertyDescription(
            """
            해당 일차의 소제목, 다른 소제목과 중복되지 않게 작성하고 재밌고 재치있게 작성하세요.
            - 예시: "오늘은 내가 만수르야!", "한옥 골목 속 숨은 서울 찾기", "도심 속 자연과 한 몸 되기", "너도 나도 이제 먹방 유튜버?!"
            """)
        String title,
    @JsonPropertyDescription(
            """
            해당 일차의 요약 설명입니다. 마크다운 형식으로 작성하되 다음 규칙을 준수하세요:
            1. 쌍따옴표(") 사용 금지, 작은따옴표(') 사용
            2. ID 값 작성 금지
            3. 유저가 이해하기 쉽도록 BulletPoint + Nutshell 사용
            4. 아래 형식으로 작성:

            **n일차 - {제목}**
            - **{일정 카테고리}**: {장소 이름}
              {요약 설명}
            - **{일정 카테고리}**: {장소 이름}
              {요약 설명}
            - **{일정 카테고리}**: {장소 이름}
              {요약 설명}

            {전체 일정 요약}: Optional

            예시:
            **1일차 - 제주 동부 해안 탐방**
            - **오전 일정**: 성산일출봉
              제주를 대표하는 일출 명소로, 유네스코 세계자연유산입니다. 정상까지 약 30분 소요됩니다.
            - **오후 일정**: 섭지코지
              드라마 촬영지로 유명한 아름다운 해안가입니다. 카페에서 바다를 바라보며 휴식을 취할 수 있습니다.
            - **저녁 일정**: 성산 해녀의집
              신선한 해산물 정식을 맛볼 수 있는 현지 맛집입니다.

            제주의 동부 해안을 따라 자연 경관을 감상하며 여유로운 하루를 보냅니다.
            """)
        String description,
    @JsonPropertyDescription("방문 장소 배열") List<AiScheduledPlace> places) {

  /** AI가 생성한 방문 장소 DTO */
  @Builder
  public record AiScheduledPlace(
      @JsonPropertyDescription("DB에 저장된 장소 식별자 ID, null 값을 가질 수 없음") Long placeId,
      @JsonPropertyDescription("장소 한 줄 요약") String summary,
      @JsonPropertyDescription("일정 방문 순서 (1부터 시작)") int visitOrder,
      @JsonPropertyDescription("일정 카테고리") ScheduledCategory category,
      @JsonPropertyDescription("다음 장소까지 예상 이동 시간(분)") int travelTime,
      @JsonPropertyDescription("이동 수단") Transportation transportation) {}
}
